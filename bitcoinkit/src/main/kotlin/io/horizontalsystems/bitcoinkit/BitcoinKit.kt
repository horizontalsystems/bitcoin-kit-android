package io.horizontalsystems.bitcoinkit

import android.content.Context
import io.horizontalsystems.bitcoinkit.blocks.BlockSyncer
import io.horizontalsystems.bitcoinkit.blocks.Blockchain
import io.horizontalsystems.bitcoinkit.core.DataProvider
import io.horizontalsystems.bitcoinkit.core.KitStateProvider
import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.managers.*
import io.horizontalsystems.bitcoinkit.models.BitcoinPaymentData
import io.horizontalsystems.bitcoinkit.models.BlockInfo
import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.models.TransactionInfo
import io.horizontalsystems.bitcoinkit.network.*
import io.horizontalsystems.bitcoinkit.network.peer.PeerGroup
import io.horizontalsystems.bitcoinkit.network.peer.PeerHostManager
import io.horizontalsystems.bitcoinkit.transactions.*
import io.horizontalsystems.bitcoinkit.transactions.builder.TransactionBuilder
import io.horizontalsystems.bitcoinkit.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoinkit.utils.AddressConverter
import io.horizontalsystems.bitcoinkit.utils.PaymentAddressParser
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.realm.Realm
import io.realm.annotations.RealmModule

@RealmModule(library = true, allClasses = true)
class BitcoinKitModule

class BitcoinKit(words: List<String>, networkType: NetworkType, peerSize: Int = 10, newWallet: Boolean = false, confirmationsThreshold: Int = 6) : KitStateProvider.Listener, DataProvider.Listener {

    interface Listener {
        fun onTransactionsUpdate(bitcoinKit: BitcoinKit, inserted: List<TransactionInfo>, updated: List<TransactionInfo>, deleted: List<Int>)
        fun onBalanceUpdate(bitcoinKit: BitcoinKit, balance: Long)
        fun onLastBlockInfoUpdate(bitcoinKit: BitcoinKit, blockInfo: BlockInfo)
        fun onKitStateUpdate(bitcoinKit: BitcoinKit, state: KitState)
    }

    var listener: Listener? = null

    //  DataProvider getters
    val balance get() = dataProvider.balance
    val transactions get() = dataProvider.transactions
    val lastBlockHeight get() = dataProvider.lastBlockHeight

    private val peerGroup: PeerGroup
    private val initialSyncer: InitialSyncer
    private val feeRateSyncer: FeeRateSyncer
    private val addressManager: AddressManager
    private val addressConverter: AddressConverter
    private val paymentAddressParser: PaymentAddressParser
    private val transactionCreator: TransactionCreator
    private val transactionBuilder: TransactionBuilder
    private val dataProvider: DataProvider
    private val unspentOutputProvider: UnspentOutputProvider
    private val realmFactory = RealmFactory("bitcoinkit-${networkType.name}-${(words.first() + words.last()).hashCode()}")

    private val network = when (networkType) {
        NetworkType.MainNet -> MainNet()
        NetworkType.MainNetBitCash -> MainNetBitcoinCash()
        NetworkType.TestNet -> TestNet()
        NetworkType.TestNetBitCash -> TestNetBitcoinCash()
        NetworkType.RegTest -> RegTest()
    }

    init {
        val realm = realmFactory.realm
        val wallet = HDWallet(Mnemonic().toSeed(words), network.coinType)

        unspentOutputProvider = UnspentOutputProvider(realmFactory, confirmationsThreshold)
        dataProvider = DataProvider(realm, this, unspentOutputProvider)
        addressConverter = AddressConverter(network)
        addressManager = AddressManager(realmFactory, wallet, addressConverter)

        val kitStateProvider = KitStateProvider(this)
        val peerHostManager = PeerHostManager(network, realmFactory)
        val transactionLinker = TransactionLinker()
        val transactionExtractor = TransactionExtractor(addressConverter)
        val transactionProcessor = TransactionProcessor(transactionExtractor, transactionLinker, addressManager)
        val bloomFilterManager = BloomFilterManager(realmFactory)
        val addressSelector: IAddressSelector

        peerGroup = PeerGroup(peerHostManager, bloomFilterManager, network, peerSize = peerSize)
        peerGroup.blockSyncer = BlockSyncer(realmFactory, Blockchain(network), transactionProcessor, addressManager, bloomFilterManager, kitStateProvider, network)
        peerGroup.transactionSyncer = TransactionSyncer(realmFactory, transactionProcessor, addressManager, bloomFilterManager)
        peerGroup.syncStateListener = kitStateProvider

        when (networkType) {
            NetworkType.MainNet,
            NetworkType.TestNet,
            NetworkType.RegTest -> {
                addressSelector = BitcoinAddressSelector(addressConverter)
                paymentAddressParser = PaymentAddressParser("bitcoin", removeScheme = true)
            }
            NetworkType.MainNetBitCash,
            NetworkType.TestNetBitCash -> {
                addressSelector = BitcoinCashAddressSelector(addressConverter)
                paymentAddressParser = PaymentAddressParser("bitcoincash", removeScheme = false)
            }
        }

        val stateManager = StateManager(realmFactory, network, newWallet)
        val initialSyncerApi = InitialSyncerApi(wallet, addressSelector, network)

        feeRateSyncer = FeeRateSyncer(realmFactory, ApiFeeRate(networkType))
        initialSyncer = InitialSyncer(realmFactory, initialSyncerApi, stateManager, addressManager, peerGroup, kitStateProvider)
        transactionBuilder = TransactionBuilder(realmFactory, addressConverter, wallet, network, addressManager, unspentOutputProvider)
        transactionCreator = TransactionCreator(realmFactory, transactionBuilder, transactionProcessor, peerGroup)
    }

    //
    // API methods
    //
    fun start() {
        initialSyncer.sync()
        feeRateSyncer.start()
    }

    fun refresh() {
        start()
    }

    fun fee(value: Int, address: String? = null, senderPay: Boolean = true): Int {
        return transactionBuilder.fee(value, dataProvider.feeRate.medium, senderPay, address)
    }

    fun send(address: String, value: Int, senderPay: Boolean = true) {
        transactionCreator.create(address, value, dataProvider.feeRate.medium, senderPay)
    }

    fun receiveAddress(): String {
        return addressManager.receiveAddress()
    }

    fun validateAddress(address: String) {
        addressConverter.convert(address)
    }

    fun parsePaymentAddress(paymentAddress: String): BitcoinPaymentData {
        return paymentAddressParser.parse(paymentAddress)
    }

    fun clear() {
        dataProvider.clear()
        initialSyncer.stop()
        feeRateSyncer.stop()

        realmFactory.realm.use { realm ->
            realm.executeTransaction { it.deleteAll() }
        }
    }

    fun showDebugInfo() {
        addressManager.fillGap()
        realmFactory.realm.use { realm ->
            realm.where(PublicKey::class.java).findAll().forEach { pubKey ->
                try {
                    val scriptType = if (network is MainNetBitcoinCash || network is TestNetBitcoinCash)
                        ScriptType.P2PKH else
                        ScriptType.P2WPKH

                    val legacy = addressConverter.convert(pubKey.publicKeyHash, ScriptType.P2PKH).string
                    val wpkh = addressConverter.convert(pubKey.scriptHashP2WPKH, ScriptType.P2SH).string
                    val bechAddress = try {
                        addressConverter.convert(OpCodes.push(0) + OpCodes.push(pubKey.publicKeyHash), scriptType).string
                    } catch (e: Exception) {
                        ""
                    }
                    println("${pubKey.index} --- extrnl: ${pubKey.external} --- hash: ${pubKey.publicKeyHex} --- p2wkph(SH) hash: ${pubKey.scriptHashP2WPKH.toHexString()}")
                    println("legacy: $legacy --- bech32: $bechAddress --- SH(WPKH): $wpkh")
                } catch (e: Exception) {
                    println(e.message)
                }
            }
        }
    }

    //
    // DataProvider Listener implementations
    //
    override fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>, deleted: List<Int>) {
        listener?.onTransactionsUpdate(this, inserted, updated, deleted)
    }

    override fun onBalanceUpdate(balance: Long) {
        listener?.onBalanceUpdate(this, balance)
    }

    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        listener?.onLastBlockInfoUpdate(this, blockInfo)
    }

    //
    // KitStateProvider Listener implementations
    //
    override fun onKitStateUpdate(state: KitState) {
        listener?.onKitStateUpdate(this, state)
    }

    enum class NetworkType {
        MainNet,
        TestNet,
        RegTest,
        MainNetBitCash,
        TestNetBitCash
    }

    sealed class KitState {
        object Synced : KitState()
        object NotSynced : KitState()
        class Syncing(val progress: Double) : KitState()
    }

    companion object {
        fun init(context: Context) {
            Realm.init(context)
        }
    }
}
