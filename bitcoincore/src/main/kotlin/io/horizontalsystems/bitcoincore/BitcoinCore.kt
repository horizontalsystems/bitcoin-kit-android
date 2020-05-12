package io.horizontalsystems.bitcoincore

import android.content.Context
import io.horizontalsystems.bitcoincore.blocks.*
import io.horizontalsystems.bitcoincore.blocks.validators.IBlockValidator
import io.horizontalsystems.bitcoincore.core.*
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.managers.*
import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.network.messages.*
import io.horizontalsystems.bitcoincore.network.peer.*
import io.horizontalsystems.bitcoincore.serializers.BlockHeaderParser
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.*
import io.horizontalsystems.bitcoincore.transactions.builder.*
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.*
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.Single
import java.util.*
import java.util.concurrent.Executor
import kotlin.collections.LinkedHashMap

class BitcoinCoreBuilder {

    val addressConverter = AddressConverterChain()

    // required parameters
    private var context: Context? = null
    private var seed: ByteArray? = null
    private var words: List<String>? = null
    private var network: Network? = null
    private var paymentAddressParser: PaymentAddressParser? = null
    private var storage: IStorage? = null
    private var initialSyncApi: IInitialSyncApi? = null
    private var bip: Bip = Bip.BIP44
    private var blockHeaderHasher: IHasher? = null
    private var transactionInfoConverter: ITransactionInfoConverter? = null
    private var blockValidator: IBlockValidator? = null

    // parameters with default values
    private var confirmationsThreshold = 6
    private var syncMode: BitcoinCore.SyncMode = BitcoinCore.SyncMode.Api()
    private var peerSize = 10
    private val plugins = mutableListOf<IPlugin>()

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

    fun setBip(bip: Bip): BitcoinCoreBuilder {
        this.bip = bip
        return this
    }

    fun setPaymentAddressParser(paymentAddressParser: PaymentAddressParser): BitcoinCoreBuilder {
        this.paymentAddressParser = paymentAddressParser
        return this
    }

    fun setConfirmationThreshold(confirmationsThreshold: Int): BitcoinCoreBuilder {
        this.confirmationsThreshold = confirmationsThreshold
        return this
    }

    fun setSyncMode(syncMode: BitcoinCore.SyncMode): BitcoinCoreBuilder {
        this.syncMode = syncMode
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

    fun setInitialSyncApi(initialSyncApi: IInitialSyncApi?): BitcoinCoreBuilder {
        this.initialSyncApi = initialSyncApi
        return this
    }

    fun setTransactionInfoConverter(transactionInfoConverter: ITransactionInfoConverter): BitcoinCoreBuilder {
        this.transactionInfoConverter = transactionInfoConverter
        return this
    }

    fun setBlockValidator(blockValidator: IBlockValidator): BitcoinCoreBuilder {
        this.blockValidator = blockValidator
        return this
    }

    fun addPlugin(plugin: IPlugin): BitcoinCoreBuilder {
        plugins.add(plugin)
        return this
    }

    fun build(): BitcoinCore {
        val context = checkNotNull(this.context)
        val seed = checkNotNull(this.seed ?: words?.let { Mnemonic().toSeed(it) })
        val network = checkNotNull(this.network)
        val paymentAddressParser = checkNotNull(this.paymentAddressParser)
        val storage = checkNotNull(this.storage)
        val initialSyncApi = checkNotNull(this.initialSyncApi)
        val blockHeaderHasher = this.blockHeaderHasher ?: DoubleSha256Hasher()
        val transactionInfoConverter = this.transactionInfoConverter ?: TransactionInfoConverter()

        val restoreKeyConverterChain = RestoreKeyConverterChain()

        val pluginManager = PluginManager()
        plugins.forEach { pluginManager.addPlugin(it) }

        restoreKeyConverterChain.add(pluginManager)

        transactionInfoConverter.baseConverter = BaseTransactionInfoConverter(pluginManager)

        val unspentOutputProvider = UnspentOutputProvider(storage, confirmationsThreshold, pluginManager)

        val dataProvider = DataProvider(storage, unspentOutputProvider, transactionInfoConverter)

        val connectionManager = ConnectionManager(context)

        val hdWallet = HDWallet(seed, network.coinType, purpose = bip.purpose)

        val publicKeyManager = PublicKeyManager.create(storage, hdWallet, restoreKeyConverterChain)
        val pendingOutpointsProvider = PendingOutpointsProvider(storage)

        val irregularOutputFinder = IrregularOutputFinder(storage)
        val transactionOutputsCache = OutputsCache.create(storage)
        val transactionExtractor = TransactionExtractor(addressConverter, storage, pluginManager)
        val transactionMediator = TransactionMediator()

        val transactionProcessor = TransactionProcessor(storage, transactionExtractor, transactionOutputsCache, publicKeyManager, irregularOutputFinder, dataProvider, transactionInfoConverter, transactionMediator)

        val kitStateProvider = KitStateProvider()

        val peerHostManager = PeerAddressManager(network, storage)
        val bloomFilterManager = BloomFilterManager()

        val peerManager = PeerManager()

        val networkMessageParser = NetworkMessageParser(network.magic)
        val networkMessageSerializer = NetworkMessageSerializer(network.magic)

        val blockchain = Blockchain(storage, blockValidator, dataProvider)
        val checkpoint = BlockSyncer.resolveCheckpoint(syncMode, network, storage)

        val blockSyncer = BlockSyncer(storage, blockchain, transactionProcessor, publicKeyManager, kitStateProvider, checkpoint)
        val initialBlockDownload = InitialBlockDownload(blockSyncer, peerManager, kitStateProvider, MerkleBlockExtractor(network.maxBlockSize))
        val peerGroup = PeerGroup(peerHostManager, network, peerManager, peerSize, networkMessageParser, networkMessageSerializer, connectionManager, blockSyncer.localDownloadedBestBlockHeight)
        peerHostManager.listener = peerGroup

        val transactionSyncer = TransactionSyncer(storage, transactionProcessor, publicKeyManager)
        val transactionSendTimer = TransactionSendTimer(60)
        val transactionSender = TransactionSender(transactionSyncer, peerGroup, storage, transactionSendTimer).apply {
            transactionSendTimer.listener = this
        }

        val transactionDataSorterFactory = TransactionDataSorterFactory()
        val unspentOutputSelector = UnspentOutputSelectorChain()
        val transactionSizeCalculator = TransactionSizeCalculator()
        val inputSigner = InputSigner(hdWallet, network)
        val outputSetter = OutputSetter(transactionDataSorterFactory)
        val dustCalculator = DustCalculator(network.dustRelayTxFee, transactionSizeCalculator)
        val inputSetter = InputSetter(unspentOutputSelector, publicKeyManager, addressConverter, bip.scriptType, transactionSizeCalculator, pluginManager, dustCalculator, transactionDataSorterFactory)
        val signer = TransactionSigner(inputSigner)
        val lockTimeSetter = LockTimeSetter(storage)
        val recipientSetter = RecipientSetter(addressConverter, pluginManager)
        val transactionBuilder = TransactionBuilder(recipientSetter, outputSetter, inputSetter, signer, lockTimeSetter)
        val transactionFeeCalculator = TransactionFeeCalculator(recipientSetter, inputSetter, addressConverter, publicKeyManager, bip.scriptType)
        val transactionCreator = TransactionCreator(transactionBuilder, transactionProcessor, transactionSender, bloomFilterManager)

        val blockHashFetcher = BlockHashFetcher(restoreKeyConverterChain, initialSyncApi, BlockHashFetcherHelper())
        val blockDiscovery = BlockDiscoveryBatch(Wallet(hdWallet), blockHashFetcher, checkpoint.block.height)
        val stateManager = StateManager(storage, network.syncableFromApi && syncMode is BitcoinCore.SyncMode.Api)
        val errorStorage = ErrorStorage()
        val initialSyncer = InitialSyncer(storage, blockDiscovery, publicKeyManager, errorStorage)

        val syncManager = SyncManager(peerGroup, initialSyncer, kitStateProvider, stateManager, connectionManager)
        initialSyncer.listener = syncManager
        connectionManager.listener = syncManager

        val bitcoinCore = BitcoinCore(
                storage,
                dataProvider,
                publicKeyManager,
                addressConverter,
                restoreKeyConverterChain,
                kitStateProvider,
                transactionCreator,
                transactionFeeCalculator,
                paymentAddressParser,
                syncManager,
                bip,
                peerManager,
                dustCalculator,
                pluginManager,
                errorStorage)

        dataProvider.listener = bitcoinCore
        kitStateProvider.listener = bitcoinCore

        val watchedTransactionManager = WatchedTransactionManager()
        bloomFilterManager.addBloomFilterProvider(watchedTransactionManager)
        bloomFilterManager.addBloomFilterProvider(publicKeyManager)
        bloomFilterManager.addBloomFilterProvider(pendingOutpointsProvider)
        bloomFilterManager.addBloomFilterProvider(irregularOutputFinder)

        bitcoinCore.watchedTransactionManager = watchedTransactionManager
        transactionProcessor.listener = watchedTransactionManager

        bitcoinCore.peerGroup = peerGroup
        bitcoinCore.transactionSyncer = transactionSyncer
        bitcoinCore.networkMessageParser = networkMessageParser
        bitcoinCore.networkMessageSerializer = networkMessageSerializer
        bitcoinCore.unspentOutputSelector = unspentOutputSelector

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
                .addMessageParser(RejectMessageParser())

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

        val bloomFilterLoader = BloomFilterLoader(bloomFilterManager, peerManager)
        bloomFilterManager.listener = bloomFilterLoader
        bitcoinCore.addPeerGroupListener(bloomFilterLoader)

        // todo: now this part cannot be moved to another place since bitcoinCore requires initialBlockDownload to be set. find solution to do so
        bitcoinCore.initialBlockDownload = initialBlockDownload
        bitcoinCore.addPeerTaskHandler(initialBlockDownload)
        bitcoinCore.addInventoryItemsHandler(initialBlockDownload)
        bitcoinCore.addPeerGroupListener(initialBlockDownload)

        bitcoinCore.addPeerSyncListener(SendTransactionsOnPeersSynced(transactionSender))

        val mempoolTransactions = MempoolTransactions(transactionSyncer, transactionSender)
        bitcoinCore.addPeerTaskHandler(mempoolTransactions)
        bitcoinCore.addInventoryItemsHandler(mempoolTransactions)
        bitcoinCore.addPeerGroupListener(mempoolTransactions)

        bitcoinCore.addPeerTaskHandler(transactionSender)

        bitcoinCore.prependUnspentOutputSelector(UnspentOutputSelector(transactionSizeCalculator, unspentOutputProvider))
        bitcoinCore.prependUnspentOutputSelector(UnspentOutputSelectorSingleNoChange(transactionSizeCalculator, unspentOutputProvider))

        return bitcoinCore
    }
}

class BitcoinCore(
        private val storage: IStorage,
        private val dataProvider: DataProvider,
        private val publicKeyManager: PublicKeyManager,
        private val addressConverter: AddressConverterChain,
        private val restoreKeyConverterChain: RestoreKeyConverterChain,
        private val kitStateProvider: KitStateProvider,
        private val transactionCreator: TransactionCreator,
        private val transactionFeeCalculator: TransactionFeeCalculator,
        private val paymentAddressParser: PaymentAddressParser,
        private val syncManager: SyncManager,
        private val bip: Bip,
        private var peerManager: PeerManager,
        private val dustCalculator: DustCalculator,
        private val pluginManager: PluginManager,
        private val errorStorage: ErrorStorage)
    : KitStateProvider.Listener, DataProvider.Listener {

    interface Listener {
        fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>) = Unit
        fun onTransactionsDelete(hashes: List<String>) = Unit
        fun onBalanceUpdate(balance: BalanceInfo) = Unit
        fun onLastBlockInfoUpdate(blockInfo: BlockInfo) = Unit
        fun onKitStateUpdate(state: KitState) = Unit
    }

    // START: Extending
    lateinit var peerGroup: PeerGroup
    lateinit var transactionSyncer: TransactionSyncer
    lateinit var networkMessageParser: NetworkMessageParser
    lateinit var networkMessageSerializer: NetworkMessageSerializer
    lateinit var initialBlockDownload: InitialBlockDownload
    lateinit var unspentOutputSelector: UnspentOutputSelectorChain
    lateinit var watchedTransactionManager: WatchedTransactionManager

    val inventoryItemsHandlerChain = InventoryItemsHandlerChain()
    val peerTaskHandlerChain = PeerTaskHandlerChain()

    fun addPeerSyncListener(peerSyncListener: IPeerSyncListener): BitcoinCore {
        initialBlockDownload.addPeerSyncListener(peerSyncListener)
        return this
    }

    fun addRestoreKeyConverter(keyConverter: IRestoreKeyConverter) {
        restoreKeyConverterChain.add(keyConverter)
    }

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

    fun addPeerGroupListener(listener: PeerGroup.Listener) {
        peerGroup.addPeerGroupListener(listener)
    }

    fun prependUnspentOutputSelector(selector: IUnspentOutputSelector) {
        unspentOutputSelector.prependSelector(selector)
    }

    fun prependAddressConverter(converter: IAddressConverter) {
        addressConverter.prependConverter(converter)
    }

    // END: Extending

    var listenerExecutor: Executor = DirectExecutor()

    //  DataProvider getters
    val balance get() = dataProvider.balance
    val lastBlockInfo get() = dataProvider.lastBlockInfo
    val syncState get() = kitStateProvider.syncState

    var listener: Listener? = null

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

    fun refresh() {
        start()
    }

    fun transactions(fromUid: String? = null, limit: Int? = null): Single<List<TransactionInfo>> {
        return dataProvider.transactions(fromUid, limit)
    }

    fun fee(value: Long, address: String? = null, senderPay: Boolean = true, feeRate: Int, pluginData: Map<Byte, IPluginData>): Long {
        return transactionFeeCalculator.fee(value, feeRate, senderPay, address, pluginData)
    }

    fun send(address: String, value: Long, senderPay: Boolean = true, feeRate: Int, sortType: TransactionDataSortType, pluginData: Map<Byte, IPluginData>): FullTransaction {
        try {
            return transactionCreator.create(address, value, feeRate, senderPay, sortType, pluginData)
        } catch (error: Exception) {
            errorStorage.addSendError(error)
            throw error
        }
    }

    fun send(hash: ByteArray, scriptType: ScriptType, value: Long, senderPay: Boolean = true, feeRate: Int, sortType: TransactionDataSortType): FullTransaction {
        val address = addressConverter.convert(hash, scriptType)
        return transactionCreator.create(address.string, value, feeRate, senderPay, sortType, mapOf())
    }

    fun redeem(unspentOutput: UnspentOutput, address: String, feeRate: Int, sortType: TransactionDataSortType): FullTransaction {
        return transactionCreator.create(unspentOutput, address, feeRate, sortType)
    }

    fun receiveAddress(): String {
        return addressConverter.convert(publicKeyManager.receivePublicKey(), bip.scriptType).string
    }

    fun receivePublicKey(): PublicKey {
        return publicKeyManager.receivePublicKey()
    }

    fun changePublicKey(): PublicKey {
        return publicKeyManager.changePublicKey()
    }

    fun getPublicKeyByPath(path: String): PublicKey {
        return publicKeyManager.getPublicKeyByPath(path)
    }

    fun validateAddress(address: String, pluginData: Map<Byte, IPluginData> = mapOf()) {
        pluginManager.validateAddress(addressConverter.convert(address), pluginData)
    }

    fun parsePaymentAddress(paymentAddress: String): BitcoinPaymentData {
        return paymentAddressParser.parse(paymentAddress)
    }

    fun showDebugInfo() {
        publicKeyManager.fillGap()
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

    fun statusInfo(): Map<String, Any> {
        val statusInfo = LinkedHashMap<String, Any>()

        statusInfo["Synced Until"] = lastBlockInfo?.timestamp?.let { Date(it * 1000) } ?: "N/A"
        statusInfo["Syncing Peer"] = initialBlockDownload.syncPeer?.host ?: "N/A"
        statusInfo["Errors"] = errorStorage.errors
        statusInfo["Last Block Height"] = lastBlockInfo?.height ?: "N/A"

        val peers = LinkedHashMap<String, Any>()
        peerManager.connected().forEachIndexed { index, peer ->

            val peerStatus = LinkedHashMap<String, Any>()
            peerStatus["Status"] = if (peer.synced) "Synced" else "Not Synced"
            peerStatus["Host"] = peer.host
            peerStatus["Best Block"] = peer.announcedLastBlockHeight

            peer.tasks.let { peerTasks ->
                if (peerTasks.isEmpty()) {
                    peerStatus["tasks"] = "no tasks"
                } else {
                    val tasks = LinkedHashMap<String, Any>()
                    peerTasks.forEach { task ->
                        tasks[task.javaClass.simpleName] = "[${task.state}]"
                    }
                    peerStatus["tasks"] = tasks
                }
            }

            peers["Peer ${index + 1}"] = peerStatus
        }

        statusInfo.putAll(peers)

        return statusInfo
    }

    //
    // DataProvider Listener implementations
    //
    override fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>) {
        listenerExecutor.execute {
            listener?.onTransactionsUpdate(inserted, updated)
        }
    }

    override fun onTransactionsDelete(hashes: List<String>) {
        listenerExecutor.execute {
            listener?.onTransactionsDelete(hashes)
        }
    }

    override fun onBalanceUpdate(balance: BalanceInfo) {
        listenerExecutor.execute {
            listener?.onBalanceUpdate(balance)
        }
    }

    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        listenerExecutor.execute {
            listener?.onLastBlockInfoUpdate(blockInfo)
        }
    }

    //
    // KitStateProvider Listener implementations
    //
    override fun onKitStateUpdate(state: KitState) {
        listenerExecutor.execute {
            listener?.onKitStateUpdate(state)
        }
    }

    fun watchTransaction(filter: TransactionFilter, listener: WatchedTransactionManager.Listener) {
        watchedTransactionManager.add(filter, listener)
    }

    fun maximumSpendableValue(address: String?, feeRate: Int, pluginData: Map<Byte, IPluginData>): Long {
        return balance.spendable - transactionFeeCalculator.fee(balance.spendable, feeRate, false, address, pluginData)
    }

    fun minimumSpendableValue(address: String?): Int {
        // by default script type is P2PKH, since it is most used
        val scriptType = when {
            address != null -> addressConverter.convert(address).scriptType
            else -> ScriptType.P2PKH
        }

        return dustCalculator.dust(scriptType)
    }

    fun maximumSpendLimit(pluginData: Map<Byte, IPluginData>): Long? {
        return pluginManager.maximumSpendLimit(pluginData)
    }

    sealed class KitState {
        object Synced : KitState()
        class NotSynced(val exception: Throwable) : KitState()
        class Syncing(val progress: Double) : KitState()

        override fun equals(other: Any?) = when {
            this is Synced && other is Synced -> true
            this is NotSynced && other is NotSynced -> exception == other.exception
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

    sealed class SyncMode {
        class Full : SyncMode()
        class Api : SyncMode()
        class NewWallet : SyncMode()
    }

    sealed class StateError : Exception() {
        class NotStarted : StateError()
        class NoInternet : StateError()
    }

}
