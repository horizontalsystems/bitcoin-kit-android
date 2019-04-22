package io.horizontalsystems.bitcoincore

import android.content.Context
import io.horizontalsystems.bitcoincore.blocks.BlockSyncer
import io.horizontalsystems.bitcoincore.blocks.Blockchain
import io.horizontalsystems.bitcoincore.blocks.BloomFilterLoader
import io.horizontalsystems.bitcoincore.blocks.InitialBlockDownload
import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorChain
import io.horizontalsystems.bitcoincore.blocks.validators.IBlockValidator
import io.horizontalsystems.bitcoincore.blocks.validators.ProofOfWorkValidator
import io.horizontalsystems.bitcoincore.core.*
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.managers.*
import io.horizontalsystems.bitcoincore.models.BitcoinPaymentData
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.FeePriority
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.network.messages.*
import io.horizontalsystems.bitcoincore.network.peer.*
import io.horizontalsystems.bitcoincore.serializers.BlockHeaderParser
import io.horizontalsystems.bitcoincore.transactions.*
import io.horizontalsystems.bitcoincore.transactions.builder.TransactionBuilder
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.*
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.Single
import java.util.concurrent.Executor

class BitcoinCoreBuilder {

    // required parameters
    private var context: Context? = null
    private var seed: ByteArray? = null
    private var words: List<String>? = null
    private var network: Network? = null
    private var paymentAddressParser: PaymentAddressParser? = null
    private var addressSelector: IAddressSelector? = null
    private var apiFeeRateCoinCode: String? = null
    private var storage: IStorage? = null
    private var initialSyncApiUrl: String? = null

    // parameters with default values
    private var confirmationsThreshold = 6
    private var newWallet = false
    private var peerSize = 10
    private var blockHeaderHasher: IHasher? = null

    fun setContext(context: Context): BitcoinCoreBuilder {
        this.context = context
        return this
    }

    fun setSeed(seed: ByteArray): BitcoinCoreBuilder {
        this.seed = seed
        return this
    }

    fun setWords(words: List<String>): BitcoinCoreBuilder {
        this.words = words
        return this
    }

    fun setNetwork(network: Network): BitcoinCoreBuilder {
        this.network = network
        return this
    }

    fun setPaymentAddressParser(paymentAddressParser: PaymentAddressParser): BitcoinCoreBuilder {
        this.paymentAddressParser = paymentAddressParser
        return this
    }

    fun setAddressSelector(addressSelector: IAddressSelector): BitcoinCoreBuilder {
        this.addressSelector = addressSelector
        return this
    }

    fun setApiFeeRateCoinCode(apiFeeRateCoinCode: String): BitcoinCoreBuilder {
        this.apiFeeRateCoinCode = apiFeeRateCoinCode
        return this
    }

    fun setConfirmationThreshold(confirmationsThreshold: Int): BitcoinCoreBuilder {
        this.confirmationsThreshold = confirmationsThreshold
        return this
    }

    fun setNewWallet(newWallet: Boolean): BitcoinCoreBuilder {
        this.newWallet = newWallet
        return this
    }

    fun setPeerSize(peerSize: Int): BitcoinCoreBuilder {
        this.peerSize = peerSize
        return this
    }

    fun setStorage(storage: IStorage): BitcoinCoreBuilder {
        this.storage = storage
        return this
    }

    fun setBlockHeaderHasher(blockHeaderHasher: IHasher): BitcoinCoreBuilder {
        this.blockHeaderHasher = blockHeaderHasher
        return this
    }

    fun setInitialSyncApiUrl(initialSyncApiUrl: String?): BitcoinCoreBuilder {
        this.initialSyncApiUrl = initialSyncApiUrl
        return this
    }

    fun build(): BitcoinCore {
        val context = checkNotNull(this.context)
        val seed = checkNotNull(this.seed ?: words?.let { Mnemonic().toSeed(it) })
        val network = checkNotNull(this.network)
        val paymentAddressParser = checkNotNull(this.paymentAddressParser)
        val addressSelector = checkNotNull(this.addressSelector)
        val apiFeeRateCoinCode = checkNotNull(this.apiFeeRateCoinCode)
        val storage = checkNotNull(this.storage)
        val initialSyncApiUrl = this.initialSyncApiUrl ?: "http://btc-testnet.horizontalsystems.xyz/apg"
        val blockHeaderHasher = this.blockHeaderHasher ?: DoubleSha256Hasher()

        val apiFeeRate = ApiFeeRate(apiFeeRateCoinCode)

        val addressConverter = AddressConverterChain()

        val unspentOutputProvider = UnspentOutputProvider(storage, confirmationsThreshold)

        val dataProvider = DataProvider(storage, unspentOutputProvider)

        val connectionManager = ConnectionManager(context)

        val hdWallet = HDWallet(seed, network.coinType)

        val addressManager = AddressManager.create(storage, hdWallet, addressConverter)

        val transactionLinker = TransactionLinker(storage)
        val transactionExtractor = TransactionExtractor(addressConverter, storage)
        val transactionProcessor = TransactionProcessor(storage, transactionExtractor, transactionLinker, addressManager, dataProvider)

        val kitStateProvider = KitStateProvider()

        val peerHostManager = PeerAddressManager(network, storage)
        val bloomFilterManager = BloomFilterManager(storage)

        val peerManager = PeerManager()

        val networkMessageParser = NetworkMessageParser(network.magic)
        val networkMessageSerializer = NetworkMessageSerializer(network.magic)

        val peerGroup = PeerGroup(peerHostManager, network, peerManager, peerSize, networkMessageParser, networkMessageSerializer)
        peerGroup.connectionManager = connectionManager

        val transactionSyncer = TransactionSyncer(storage, transactionProcessor, addressManager, bloomFilterManager)

        val transactionSender = TransactionSender()
        transactionSender.peerGroup = peerGroup
        transactionSender.transactionSyncer = transactionSyncer

        val transactionBuilder = TransactionBuilder(addressConverter, hdWallet, network, addressManager, unspentOutputProvider)
        val transactionCreator = TransactionCreator(transactionBuilder, transactionProcessor, transactionSender)

        val feeRateSyncer = FeeRateSyncer(storage, apiFeeRate)
        val blockHashFetcher = BlockHashFetcher(addressSelector, addressConverter, BCoinApi(initialSyncApiUrl, HttpRequester()), BlockHashFetcherHelper())
        val blockDiscovery = BlockDiscoveryBatch(Wallet(hdWallet), blockHashFetcher, network.checkpointBlock.height)
        val stateManager = StateManager(storage, network, newWallet)
        val initialSyncer = InitialSyncer(storage, blockDiscovery, stateManager, addressManager, kitStateProvider)

        val syncManager = SyncManager(connectionManager, feeRateSyncer, peerGroup, initialSyncer)
        initialSyncer.listener = syncManager

        val bitcoinCore = BitcoinCore(
                storage,
                dataProvider,
                addressManager,
                addressConverter,
                kitStateProvider,
                transactionBuilder,
                transactionCreator,
                paymentAddressParser,
                syncManager)

        dataProvider.listener = bitcoinCore
        kitStateProvider.listener = bitcoinCore

        bitcoinCore.peerGroup = peerGroup
        bitcoinCore.transactionSyncer = transactionSyncer
        bitcoinCore.networkMessageParser = networkMessageParser
        bitcoinCore.networkMessageSerializer = networkMessageSerializer

        peerGroup.peerTaskHandler = bitcoinCore.peerTaskHandlerChain
        peerGroup.inventoryItemsHandler = bitcoinCore.inventoryItemsHandlerChain

        bitcoinCore.prependAddressConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))

        // this part can be moved to another place

        bitcoinCore.addMessageParser(AddrMessageParser())
                .addMessageParser(MerkleBlockMessageParser(BlockHeaderParser(blockHeaderHasher)))
                .addMessageParser(InvMessageParser())
                .addMessageParser(GetDataMessageParser())
                .addMessageParser(PingMessageParser())
                .addMessageParser(PongMessageParser())
                .addMessageParser(TransactionMessageParser())
                .addMessageParser(VerAckMessageParser())
                .addMessageParser(VersionMessageParser())

        bitcoinCore.addMessageSerializer(FilterLoadMessageSerializer())
                .addMessageSerializer(GetBlocksMessageSerializer())
                .addMessageSerializer(InvMessageSerializer())
                .addMessageSerializer(GetDataMessageSerializer())
                .addMessageSerializer(MempoolMessageSerializer())
                .addMessageSerializer(PingMessageSerializer())
                .addMessageSerializer(PongMessageSerializer())
                .addMessageSerializer(TransactionMessageSerializer())
                .addMessageSerializer(VerAckMessageSerializer())
                .addMessageSerializer(VersionMessageSerializer())

        val bloomFilterLoader = BloomFilterLoader(bloomFilterManager)
        bloomFilterManager.listener = bloomFilterLoader
        bitcoinCore.addPeerGroupListener(bloomFilterLoader)

        val initialBlockDownload = InitialBlockDownload(BlockSyncer(storage, Blockchain(storage, bitcoinCore.blockValidatorChain, dataProvider), transactionProcessor, addressManager, bloomFilterManager, kitStateProvider, network), peerManager, kitStateProvider)
        bitcoinCore.addPeerTaskHandler(initialBlockDownload)
        bitcoinCore.addInventoryItemsHandler(initialBlockDownload)
        bitcoinCore.addPeerGroupListener(initialBlockDownload)
        initialBlockDownload.peersSyncedListener = SendTransactionsOnPeersSynced(transactionSender)

        val mempoolTransactions = MempoolTransactions(transactionSyncer)
        bitcoinCore.addPeerTaskHandler(mempoolTransactions)
        bitcoinCore.addInventoryItemsHandler(mempoolTransactions)
        bitcoinCore.addPeerGroupListener(mempoolTransactions)

        return bitcoinCore
    }

}

class BitcoinCore(private val storage: IStorage, private val dataProvider: DataProvider, private val addressManager: AddressManager, private val addressConverter: AddressConverterChain, private val kitStateProvider: KitStateProvider, private val transactionBuilder: TransactionBuilder, private val transactionCreator: TransactionCreator, private val paymentAddressParser: PaymentAddressParser, private val syncManager: SyncManager)
    : KitStateProvider.Listener, DataProvider.Listener {

    interface Listener {
        fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>) = Unit
        fun onTransactionsDelete(hashes: List<ByteArray>) = Unit
        fun onBalanceUpdate(balance: Long) = Unit
        fun onLastBlockInfoUpdate(blockInfo: BlockInfo) = Unit
        fun onKitStateUpdate(state: KitState) = Unit
    }

    // START: Extending
    lateinit var peerGroup: PeerGroup
    lateinit var transactionSyncer: TransactionSyncer
    lateinit var networkMessageParser: NetworkMessageParser
    lateinit var networkMessageSerializer: NetworkMessageSerializer

    val inventoryItemsHandlerChain = InventoryItemsHandlerChain()
    val peerTaskHandlerChain = PeerTaskHandlerChain()
    val blockValidatorChain = BlockValidatorChain(ProofOfWorkValidator())

    fun addMessageParser(messageParser: IMessageParser): BitcoinCore {
        networkMessageParser.add(messageParser)
        return this
    }

    fun addMessageSerializer(messageSerializer: IMessageSerializer): BitcoinCore {
        networkMessageSerializer.add(messageSerializer)
        return this
    }

    fun addInventoryItemsHandler(handler: IInventoryItemsHandler) {
        inventoryItemsHandlerChain.addHandler(handler)
    }

    fun addPeerTaskHandler(handler: IPeerTaskHandler) {
        peerTaskHandlerChain.addHandler(handler)
    }

    fun addPeerGroupListener(listener: PeerGroup.IPeerGroupListener) {
        peerGroup.addPeerGroupListener(listener)
    }

    fun prependAddressConverter(converter: IAddressConverter) {
        addressConverter.prependConverter(converter)
    }

    fun addBlockValidator(validator: IBlockValidator) {
        blockValidatorChain.add(validator)
    }

    // END: Extending

    var listenerExecutor: Executor = DirectExecutor()

    //  DataProvider getters
    val balance get() = dataProvider.balance
    val lastBlockInfo get() = dataProvider.lastBlockInfo
    val syncState get() = kitStateProvider.syncState

    private val listeners = mutableListOf<Listener>()

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    //
    // API methods
    //
    fun start() {
        syncManager.start()
    }

    fun stop() {
        dataProvider.clear()
        syncManager.stop()
        storage.clear()
    }

    fun clear() {
        stop()
    }

    fun refresh() {
        start()
    }

    fun transactions(fromHash: String? = null, limit: Int? = null): Single<List<TransactionInfo>> {
        return dataProvider.transactions(fromHash, limit)
    }

    fun fee(value: Long, address: String? = null, senderPay: Boolean = true, feePriority: FeePriority = FeePriority.Medium): Long {
        return transactionBuilder.fee(value, getFeeRate(feePriority), senderPay, address)
    }

    fun send(address: String, value: Long, senderPay: Boolean = true, feePriority: FeePriority = FeePriority.Medium) {
        transactionCreator.create(address, value, getFeeRate(feePriority), senderPay)
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
//                    val scriptType = if (network is MainNetBitcoinCash || network is TestNetBitcoinCash)
//                        ScriptType.P2PKH else
//                        ScriptType.P2WPKH

                val legacy = addressConverter.convert(pubKey.publicKeyHash, ScriptType.P2PKH).string
//                    val wpkh = addressConverter.convert(pubKey.scriptHashP2WPKH, ScriptType.P2SH).string
//                    val bechAddress = try {
//                        addressConverter.convert(OpCodes.push(0) + OpCodes.push(pubKey.publicKeyHash), scriptType).string
//                    } catch (e: Exception) {
//                        ""
//                    }
                println("${pubKey.index} --- extrnl: ${pubKey.external} --- hash: ${pubKey.publicKeyHash.toHexString()} ---- legacy: $legacy")
//                    println("legacy: $legacy --- bech32: $bechAddress --- SH(WPKH): $wpkh")
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
            listeners.forEach {
                it.onTransactionsUpdate(inserted, updated)
            }
        }
    }

    override fun onTransactionsDelete(hashes: List<ByteArray>) {
        listenerExecutor.execute {
            listeners.forEach {
                it.onTransactionsDelete(hashes)
            }
        }
    }

    override fun onBalanceUpdate(balance: Long) {
        listenerExecutor.execute {
            listeners.forEach { it ->
                it.onBalanceUpdate(balance)
            }

        }
    }

    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        listenerExecutor.execute {
            listeners.forEach {
                it.onLastBlockInfoUpdate(blockInfo)
            }
        }
    }

    //
    // KitStateProvider Listener implementations
    //
    override fun onKitStateUpdate(state: KitState) {
        listenerExecutor.execute {
            listeners.forEach {
                it.onKitStateUpdate(state)
            }
        }
    }

    private fun getFeeRate(feePriority: FeePriority) = when (feePriority) {
        FeePriority.Lowest -> dataProvider.feeRate.lowPriority
        FeePriority.Low -> (dataProvider.feeRate.lowPriority + dataProvider.feeRate.mediumPriority) / 2
        FeePriority.Medium -> dataProvider.feeRate.mediumPriority
        FeePriority.High -> (dataProvider.feeRate.mediumPriority + dataProvider.feeRate.highPriority) / 2
        FeePriority.Highest -> dataProvider.feeRate.highPriority
        is FeePriority.Custom -> feePriority.feeRate
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

    companion object {
        const val maxTargetBits: Long = 0x1d00ffff                // Maximum difficulty

        const val targetSpacing = 10 * 60                         // 10 minutes per block.
        const val targetTimespan: Long = 14 * 24 * 60 * 60        // 2 weeks per difficulty cycle, on average.
        const val heightInterval = targetTimespan / targetSpacing // 2016 blocks
    }
}
