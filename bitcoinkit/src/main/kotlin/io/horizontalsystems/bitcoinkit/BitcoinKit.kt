package io.horizontalsystems.bitcoinkit

import android.content.Context
import android.os.Handler
import io.horizontalsystems.bitcoinkit.blocks.BlockSyncer
import io.horizontalsystems.bitcoinkit.blocks.Blockchain
import io.horizontalsystems.bitcoinkit.blocks.ProgressSyncer
import io.horizontalsystems.bitcoinkit.core.DataProvider
import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.managers.*
import io.horizontalsystems.bitcoinkit.models.BitcoinPaymentData
import io.horizontalsystems.bitcoinkit.models.BlockInfo
import io.horizontalsystems.bitcoinkit.models.TransactionInfo
import io.horizontalsystems.bitcoinkit.network.*
import io.horizontalsystems.bitcoinkit.network.peer.PeerGroup
import io.horizontalsystems.bitcoinkit.network.peer.PeerHostManager
import io.horizontalsystems.bitcoinkit.transactions.*
import io.horizontalsystems.bitcoinkit.transactions.builder.TransactionBuilder
import io.horizontalsystems.bitcoinkit.utils.AddressConverter
import io.horizontalsystems.bitcoinkit.utils.PaymentAddressParser
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.realm.Realm
import io.realm.annotations.RealmModule

@RealmModule(library = true, allClasses = true)
class BitcoinKitModule

class BitcoinKit(words: List<String>, networkType: NetworkType, peerSize: Int = 10) : ProgressSyncer.Listener, DataProvider.Listener {

    interface Listener {
        fun onTransactionsUpdate(bitcoinKit: BitcoinKit, inserted: List<TransactionInfo>, updated: List<TransactionInfo>, deleted: List<Int>)
        fun onBalanceUpdate(bitcoinKit: BitcoinKit, balance: Long)
        fun onLastBlockInfoUpdate(bitcoinKit: BitcoinKit, blockInfo: BlockInfo)
        fun onProgressUpdate(bitcoinKit: BitcoinKit, progress: Double)
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
    private val realmFactory = RealmFactory("bitcoinkit-${networkType.name}")

    private val handler = Handler()
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

        dataProvider = DataProvider(realm, this)
        addressConverter = AddressConverter(network)
        addressManager = AddressManager(realmFactory, wallet, addressConverter)

        val progressSyncer = ProgressSyncer(this)
        val peerHostManager = PeerHostManager(network, realmFactory)
        val transactionLinker = TransactionLinker()
        val transactionExtractor = TransactionExtractor(addressConverter)
        val transactionProcessor = TransactionProcessor(transactionExtractor, transactionLinker, addressManager)
        val bloomFilterManager = BloomFilterManager(realmFactory)

        peerGroup = PeerGroup(peerHostManager, bloomFilterManager, network, peerSize = peerSize)
        peerGroup.blockSyncer = BlockSyncer(realmFactory, Blockchain(network), transactionProcessor, addressManager, bloomFilterManager, progressSyncer, network)
        peerGroup.transactionSyncer = TransactionSyncer(realmFactory, transactionProcessor, addressManager, bloomFilterManager)
        peerGroup.lastBlockHeightListener = progressSyncer

        val addressSelector: IAddressSelector
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

        val stateManager = StateManager(realmFactory, network)
        val initialSyncerApi = InitialSyncerApi(wallet, addressSelector, network)

        feeRateSyncer = FeeRateSyncer(realmFactory, ApiFeeRate(networkType))
        initialSyncer = InitialSyncer(realmFactory, initialSyncerApi, stateManager, addressManager, peerGroup)
        transactionBuilder = TransactionBuilder(realmFactory, addressConverter, wallet, network, addressManager)
        transactionCreator = TransactionCreator(realmFactory, transactionBuilder, transactionProcessor, peerGroup)
    }

    //
    // API methods
    //
    fun start() {
        initialSyncer.sync()
        feeRateSyncer.start()
    }

    fun parsePaymentAddress(paymentAddress: String): BitcoinPaymentData {
        return paymentAddressParser.parse(paymentAddress)
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

    fun clear() = realmFactory.realm.use { realm ->
        realm.executeTransaction { realm.deleteAll() }
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
    // ProgressSyncer Listener implementations
    //
    override fun onProgressUpdate(progress: Double) {
        handler.post { listener?.onProgressUpdate(this, progress) }
    }

    enum class NetworkType {
        MainNet,
        TestNet,
        RegTest,
        MainNetBitCash,
        TestNetBitCash
    }

    companion object {
        fun init(context: Context) {
            Realm.init(context)
        }
    }
}
