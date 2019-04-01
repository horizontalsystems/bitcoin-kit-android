package io.horizontalsystems.bitcoinkit

import android.content.Context
import io.horizontalsystems.bitcoinkit.blocks.BlockSyncer
import io.horizontalsystems.bitcoinkit.blocks.Blockchain
import io.horizontalsystems.bitcoinkit.core.DataProvider
import io.horizontalsystems.bitcoinkit.core.KitStateProvider
import io.horizontalsystems.bitcoinkit.core.Wallet
import io.horizontalsystems.bitcoinkit.managers.*
import io.horizontalsystems.bitcoinkit.models.BitcoinPaymentData
import io.horizontalsystems.bitcoinkit.models.BlockInfo
import io.horizontalsystems.bitcoinkit.models.TransactionInfo
import io.horizontalsystems.bitcoinkit.network.*
import io.horizontalsystems.bitcoinkit.network.messages.BitcoinMessageParser
import io.horizontalsystems.bitcoinkit.network.messages.Message
import io.horizontalsystems.bitcoinkit.network.messages.MessageParserChain
import io.horizontalsystems.bitcoinkit.network.peer.PeerAddressManager
import io.horizontalsystems.bitcoinkit.network.peer.PeerGroup
import io.horizontalsystems.bitcoinkit.serializers.BlockHeaderSerializer
import io.horizontalsystems.bitcoinkit.storage.KitDatabase
import io.horizontalsystems.bitcoinkit.storage.Storage
import io.horizontalsystems.bitcoinkit.transactions.*
import io.horizontalsystems.bitcoinkit.transactions.builder.TransactionBuilder
import io.horizontalsystems.bitcoinkit.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoinkit.utils.*
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.Single
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class BitcoinKitBuilder {

    // required parameters
    private var context: Context? = null
    private var seed: ByteArray? = null
    private var words: List<String>? = null
    private var networkType: BitcoinKit.NetworkType? = null
    private var walletId: String? = null

    // parameters with default values
    private var confirmationsThreshold = 6
    private var newWallet = false
    private var peerSize = 10

    fun setContext(context: Context): BitcoinKitBuilder {
        this.context = context
        return this
    }

    fun setSeed(seed: ByteArray): BitcoinKitBuilder {
        this.seed = seed
        return this
    }

    fun setWords(words: List<String>): BitcoinKitBuilder {
        this.words = words
        return this
    }

    fun setNetworkType(networkType: BitcoinKit.NetworkType): BitcoinKitBuilder {
        this.networkType = networkType
        return this
    }

    fun setWalletId(walletId: String): BitcoinKitBuilder {
        this.walletId = walletId
        return this
    }

    fun setConfirmationThreshold(confirmationsThreshold: Int): BitcoinKitBuilder {
        this.confirmationsThreshold = confirmationsThreshold
        return this
    }

    fun setNewWallet(newWallet: Boolean): BitcoinKitBuilder {
        this.newWallet = newWallet
        return this
    }

    fun setPeerSize(peerSize: Int): BitcoinKitBuilder {
        this.peerSize = peerSize
        return this
    }

    fun build(): BitcoinKit {
        val context = this.context
        val seed = this.seed ?: words?.let { Mnemonic().toSeed(it) }
        val networkType = this.networkType
        val walletId = this.walletId

        checkNotNull(context)
        checkNotNull(seed)
        checkNotNull(networkType)
        checkNotNull(walletId)

        val dbName = "bitcoinkit-${networkType.name}-$walletId"
        val database = KitDatabase.getInstance(context, dbName)
        val storage = Storage(database)

        val network: Network = when (networkType) {
            BitcoinKit.NetworkType.MainNet -> MainNet(storage)
            BitcoinKit.NetworkType.MainNetBitCash -> MainNetBitcoinCash(storage)
            BitcoinKit.NetworkType.TestNet -> TestNet(storage)
            BitcoinKit.NetworkType.TestNetBitCash -> TestNetBitcoinCash(storage)
            BitcoinKit.NetworkType.RegTest -> RegTest(storage)
        }

        BlockHeaderSerializer.network = network

        val unspentOutputProvider = UnspentOutputProvider(storage, confirmationsThreshold)

        val dataProvider = DataProvider(storage, unspentOutputProvider)

        val connectionManager = ConnectionManager(context)

        val hdWallet = HDWallet(seed, network.coinType)

        val messageParserChain = MessageParserChain()
        messageParserChain.addParser(BitcoinMessageParser())

        Message.Builder.messageParser = messageParserChain

        val addressConverter = AddressConverterChain()
        addressConverter.prependConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))

        when (networkType) {
            BitcoinKit.NetworkType.MainNet,
            BitcoinKit.NetworkType.TestNet -> SegwitAddressConverter(network.addressSegwitHrp)
            BitcoinKit.NetworkType.MainNetBitCash,
            BitcoinKit.NetworkType.TestNetBitCash -> CashAddressConverter(network.addressSegwitHrp)
            else -> null
        }?.let { bech32 ->
            addressConverter.prependConverter(bech32)
        }

        val addressManager = AddressManager.create(storage, hdWallet, addressConverter)

        val transactionLinker = TransactionLinker(storage)
        val transactionExtractor = TransactionExtractor(addressConverter, storage)
        val transactionProcessor = TransactionProcessor(storage, transactionExtractor, transactionLinker, addressManager, dataProvider)

        val kitStateProvider = KitStateProvider()

        val peerHostManager = PeerAddressManager(network, storage)
        val bloomFilterManager = BloomFilterManager(storage)

        val peerGroup = PeerGroup(peerHostManager, bloomFilterManager, network, kitStateProvider, peerSize)
        peerGroup.blockSyncer = BlockSyncer(storage, Blockchain(storage, network, dataProvider), transactionProcessor, addressManager, bloomFilterManager, kitStateProvider, network)
        peerGroup.transactionSyncer = TransactionSyncer(storage, transactionProcessor, addressManager, bloomFilterManager)
        peerGroup.connectionManager = connectionManager

        val transactionBuilder = TransactionBuilder(addressConverter, hdWallet, network, addressManager, unspentOutputProvider)
        val transactionCreator = TransactionCreator(transactionBuilder, transactionProcessor, peerGroup)

        val paymentAddressParser = when (networkType) {
            BitcoinKit.NetworkType.MainNet,
            BitcoinKit.NetworkType.TestNet,
            BitcoinKit.NetworkType.RegTest -> {
                PaymentAddressParser("bitcoin", removeScheme = true)
            }
            BitcoinKit.NetworkType.MainNetBitCash,
            BitcoinKit.NetworkType.TestNetBitCash -> {
                PaymentAddressParser("bitcoincash", removeScheme = false)
            }
        }

        val addressSelector: IAddressSelector = when (networkType) {
            BitcoinKit.NetworkType.MainNet,
            BitcoinKit.NetworkType.TestNet,
            BitcoinKit.NetworkType.RegTest -> {
                BitcoinAddressSelector()
            }
            BitcoinKit.NetworkType.MainNetBitCash,
            BitcoinKit.NetworkType.TestNetBitCash -> {
                BitcoinCashAddressSelector()
            }
        }

        val apiFeeRateResource = when (networkType) {
            BitcoinKit.NetworkType.MainNet -> "BTC"
            BitcoinKit.NetworkType.TestNet -> "BTC/testnet"
            BitcoinKit.NetworkType.RegTest -> "BTC/regtest"
            BitcoinKit.NetworkType.MainNetBitCash -> "BCH"
            BitcoinKit.NetworkType.TestNetBitCash -> "BCH/testnet"
        }

        val feeRateSyncer = FeeRateSyncer(storage, ApiFeeRate(apiFeeRateResource))
        val blockHashFetcher = BlockHashFetcher(addressSelector, addressConverter, BCoinApi(network, HttpRequester()), BlockHashFetcherHelper())
        val blockDiscovery = BlockDiscoveryBatch(Wallet(hdWallet), blockHashFetcher, network.checkpointBlock.height)
        val stateManager = StateManager(storage, network, newWallet)
        val initialSyncer = InitialSyncer(storage, blockDiscovery, stateManager, addressManager, kitStateProvider)

        val syncManager = SyncManager(connectionManager, feeRateSyncer, peerGroup, initialSyncer)
        initialSyncer.listener = syncManager

        val bitcoinKit = BitcoinKit(
                storage,
                dataProvider,
                addressManager,
                addressConverter,
                kitStateProvider,
                transactionBuilder,
                transactionCreator,
                paymentAddressParser,
                syncManager)

        dataProvider.listener = bitcoinKit
        kitStateProvider.listener = bitcoinKit

        return bitcoinKit
    }

}

class BitcoinKit(private val storage: Storage, private val dataProvider: DataProvider, private val addressManager: AddressManager, private val addressConverter: IAddressConverter, private val kitStateProvider: KitStateProvider, private val transactionBuilder: TransactionBuilder, private val transactionCreator: TransactionCreator, private val paymentAddressParser: PaymentAddressParser, private val syncManager: SyncManager)
    : KitStateProvider.Listener, DataProvider.Listener {

    interface Listener {
        fun onTransactionsUpdate(bitcoinKit: BitcoinKit, inserted: List<TransactionInfo>, updated: List<TransactionInfo>) = Unit
        fun onTransactionsDelete(hashes: List<String>) = Unit
        fun onBalanceUpdate(bitcoinKit: BitcoinKit, balance: Long) = Unit
        fun onLastBlockInfoUpdate(bitcoinKit: BitcoinKit, blockInfo: BlockInfo) = Unit
        fun onKitStateUpdate(bitcoinKit: BitcoinKit, state: KitState) = Unit
    }

    var listener: Listener? = null
    var listenerExecutor: Executor = Executors.newSingleThreadExecutor()

    //  DataProvider getters
    val balance get() = dataProvider.balance
    val lastBlockInfo get() = dataProvider.lastBlockInfo
    val syncState get() = kitStateProvider.syncState

    //
    // API methods
    //
    fun start() {
        syncManager.start()
    }

    fun stop() {
        dataProvider.clear()
        syncManager.stop()
    }

    fun clear() {
        stop()
        storage.clear()
    }

    fun refresh() {
        start()
    }

    fun transactions(fromHash: String? = null, limit: Int? = null): Single<List<TransactionInfo>> {
        return dataProvider.transactions(fromHash, limit)
    }

    fun fee(value: Long, address: String? = null, senderPay: Boolean = true): Long {
        return transactionBuilder.fee(value, dataProvider.feeRate.medium, senderPay, address)
    }

    fun send(address: String, value: Long, senderPay: Boolean = true) {
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

    fun showDebugInfo() {
        addressManager.fillGap()
        storage.getPublicKeys().forEach { pubKey ->
            try {
                val scriptType = ScriptType.P2PKH

                val legacy = addressConverter.convert(pubKey.publicKeyHash, ScriptType.P2PKH).string
                val wpkh = addressConverter.convert(pubKey.scriptHashP2WPKH, ScriptType.P2SH).string
                val bechAddress = try {
                    addressConverter.convert(OpCodes.push(0) + OpCodes.push(pubKey.publicKeyHash), scriptType).string
                } catch (e: Exception) {
                    ""
                }
                println("${pubKey.index} --- extrnl: ${pubKey.external} --- hash: ${pubKey.publicKeyHex} ---- legacy: $legacy")
                println("legacy: $legacy --- bech32: $bechAddress --- SH(WPKH): $wpkh")
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    //
    // DataProvider Listener implementations
    //
    override fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>) {
        listenerExecutor.execute {
            listener?.onTransactionsUpdate(this, inserted, updated)
        }
    }

    override fun onTransactionsDelete(hashes: List<String>) {
        listenerExecutor.execute {
            listener?.onTransactionsDelete(hashes)
        }
    }

    override fun onBalanceUpdate(balance: Long) {
        listenerExecutor.execute {
            listener?.onBalanceUpdate(this, balance)
        }
    }

    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        listenerExecutor.execute {
            listener?.onLastBlockInfoUpdate(this, blockInfo)
        }
    }

    //
    // KitStateProvider Listener implementations
    //
    override fun onKitStateUpdate(state: KitState) {
        listenerExecutor.execute {
            listener?.onKitStateUpdate(this, state)
        }
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

        override fun equals(other: Any?) = when {
            this is Synced && other is Synced -> true
            this is NotSynced && other is NotSynced -> true
            this is Syncing && other is Syncing -> this.progress == other.progress
            else -> false
        }

        override fun hashCode(): Int {
            var result = javaClass.hashCode()
            if (this is Syncing) {
                result = 31 * result + progress.hashCode()
            }
            return result
        }
    }

}
