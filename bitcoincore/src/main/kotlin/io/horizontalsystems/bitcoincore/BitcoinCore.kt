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
import io.horizontalsystems.bitcoincore.transactions.extractors.MyOutputsCache
import io.horizontalsystems.bitcoincore.transactions.extractors.TransactionExtractor
import io.horizontalsystems.bitcoincore.transactions.extractors.TransactionMetadataExtractor
import io.horizontalsystems.bitcoincore.transactions.extractors.TransactionOutputProvider
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.*
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.HDWallet.Purpose
import io.horizontalsystems.hdwalletkit.HDWalletAccount
import io.horizontalsystems.hdwalletkit.HDWalletAccountWatch
import io.reactivex.Single
import java.util.*
import java.util.concurrent.Executor
import kotlin.math.roundToInt

class BitcoinCoreBuilder {

    val addressConverter = AddressConverterChain()

    // required parameters
    private var context: Context? = null
    private var extendedKey: HDExtendedKey? = null
    private var purpose: Purpose? = null
    private var network: Network? = null
    private var paymentAddressParser: PaymentAddressParser? = null
    private var storage: IStorage? = null
    private var initialSyncApi: IInitialSyncApi? = null
    private var blockHeaderHasher: IHasher? = null
    private var transactionInfoConverter: ITransactionInfoConverter? = null
    private var blockValidator: IBlockValidator? = null

    // parameters with default values
    private var confirmationsThreshold = 6
    private var syncMode: BitcoinCore.SyncMode = BitcoinCore.SyncMode.Api()
    private var peerSize = 10
    private val plugins = mutableListOf<IPlugin>()
    private var handleAddrMessage = true

    fun setContext(context: Context): BitcoinCoreBuilder {
        this.context = context
        return this
    }

    fun setExtendedKey(extendedKey: HDExtendedKey): BitcoinCoreBuilder {
        this.extendedKey = extendedKey
        return this
    }

    fun setPurpose(purpose: Purpose): BitcoinCoreBuilder {
        this.purpose = purpose
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

    fun setConfirmationThreshold(confirmationsThreshold: Int): BitcoinCoreBuilder {
        this.confirmationsThreshold = confirmationsThreshold
        return this
    }

    fun setSyncMode(syncMode: BitcoinCore.SyncMode): BitcoinCoreBuilder {
        this.syncMode = syncMode
        return this
    }

    fun setPeerSize(peerSize: Int): BitcoinCoreBuilder {
        if (peerSize < TransactionSender.minConnectedPeerSize) {
            throw Error("Peer size cannot be less than ${TransactionSender.minConnectedPeerSize}")
        }

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

    fun setHandleAddrMessage(handle: Boolean): BitcoinCoreBuilder {
        handleAddrMessage = handle
        return this
    }

    fun addPlugin(plugin: IPlugin): BitcoinCoreBuilder {
        plugins.add(plugin)
        return this
    }

    fun build(): BitcoinCore {
        val context = checkNotNull(this.context)
        val extendedKey = checkNotNull(this.extendedKey)
        val purpose = checkNotNull(this.purpose)
        val network = checkNotNull(this.network)
        val paymentAddressParser = checkNotNull(this.paymentAddressParser)
        val storage = checkNotNull(this.storage)
        val initialSyncApi = checkNotNull(this.initialSyncApi)
        val blockHeaderHasher = this.blockHeaderHasher ?: DoubleSha256Hasher()
        val transactionInfoConverter = this.transactionInfoConverter ?: TransactionInfoConverter()

        val restoreKeyConverterChain = RestoreKeyConverterChain()

        val pluginManager = PluginManager()
        plugins.forEach { pluginManager.addPlugin(it) }

        transactionInfoConverter.baseConverter = BaseTransactionInfoConverter(pluginManager)

        val unspentOutputProvider = UnspentOutputProvider(storage, confirmationsThreshold, pluginManager)

        val dataProvider = DataProvider(storage, unspentOutputProvider, transactionInfoConverter)

        val connectionManager = ConnectionManager(context)

        var privateWallet: IPrivateWallet? = null
        val publicKeyFetcher: IPublicKeyFetcher
        var multiAccountPublicKeyFetcher: IMultiAccountPublicKeyFetcher? = null
        val publicKeyManager: IPublicKeyManager
        val bloomFilterProvider: IBloomFilterProvider
        val gapLimit = 20

        if (!extendedKey.isPublic) {
            when (extendedKey.derivedType) {
                HDExtendedKey.DerivedType.Master -> {
                    val wallet = Wallet(HDWallet(extendedKey.key, network.coinType, purpose), gapLimit)
                    privateWallet = wallet
                    val fetcher = MultiAccountPublicKeyFetcher(wallet)
                    publicKeyFetcher = fetcher
                    multiAccountPublicKeyFetcher = fetcher
                    PublicKeyManager.create(storage, wallet, restoreKeyConverterChain).apply {
                        publicKeyManager = this
                        bloomFilterProvider = this
                    }
                }
                HDExtendedKey.DerivedType.Account -> {
                    val wallet = AccountWallet(HDWalletAccount(extendedKey.key), gapLimit)
                    privateWallet = wallet
                    val fetcher = PublicKeyFetcher(wallet)
                    publicKeyFetcher = fetcher
                    AccountPublicKeyManager.create(storage, wallet, restoreKeyConverterChain).apply {
                        publicKeyManager = this
                        bloomFilterProvider = this
                    }

                }
                HDExtendedKey.DerivedType.Bip32 -> {
                    throw IllegalStateException("Custom Bip32 Extended Keys are not supported")
                }
            }
        } else {
            when (extendedKey.derivedType) {
                HDExtendedKey.DerivedType.Account -> {
                    val wallet = WatchAccountWallet(HDWalletAccountWatch(extendedKey.key), gapLimit)
                    val fetcher = WatchPublicKeyFetcher(wallet)
                    publicKeyFetcher = fetcher
                    AccountPublicKeyManager.create(storage, wallet, restoreKeyConverterChain).apply {
                        publicKeyManager = this
                        bloomFilterProvider = this
                    }

                }
                HDExtendedKey.DerivedType.Bip32, HDExtendedKey.DerivedType.Master -> {
                    throw IllegalStateException("Only Account Extended Public Keys are supported")
                }
            }
        }

        val pendingOutpointsProvider = PendingOutpointsProvider(storage)

        val irregularOutputFinder = IrregularOutputFinder(storage)
        val metadataExtractor = TransactionMetadataExtractor(
            MyOutputsCache.create(storage),
            TransactionOutputProvider(storage)
        )
        val transactionExtractor = TransactionExtractor(addressConverter, storage, pluginManager, metadataExtractor)

        val conflictsResolver = TransactionConflictsResolver(storage)
        val pendingTransactionProcessor = PendingTransactionProcessor(
            storage,
            transactionExtractor,
            publicKeyManager,
            irregularOutputFinder,
            dataProvider,
            conflictsResolver
        )
        val invalidator = TransactionInvalidator(storage, transactionInfoConverter, dataProvider)
        val blockTransactionProcessor = BlockTransactionProcessor(
            storage,
            transactionExtractor,
            publicKeyManager,
            irregularOutputFinder,
            dataProvider,
            conflictsResolver,
            invalidator
        )

        val peerHostManager = PeerAddressManager(network, storage)
        val bloomFilterManager = BloomFilterManager()

        val peerManager = PeerManager()

        val networkMessageParser = NetworkMessageParser(network.magic)
        val networkMessageSerializer = NetworkMessageSerializer(network.magic)

        val blockchain = Blockchain(storage, blockValidator, dataProvider)
        val checkpoint = BlockSyncer.resolveCheckpoint(syncMode, network, storage)

        val blockSyncer = BlockSyncer(storage, blockchain, blockTransactionProcessor, publicKeyManager, checkpoint)
        val initialBlockDownload = InitialBlockDownload(blockSyncer, peerManager, MerkleBlockExtractor(network.maxBlockSize))
        val peerGroup = PeerGroup(
            peerHostManager,
            network,
            peerManager,
            peerSize,
            networkMessageParser,
            networkMessageSerializer,
            connectionManager,
            blockSyncer.localDownloadedBestBlockHeight,
            handleAddrMessage
        )
        peerHostManager.listener = peerGroup

        val unspentOutputSelector = UnspentOutputSelectorChain()
        val transactionSyncer = TransactionSyncer(storage, pendingTransactionProcessor, invalidator, publicKeyManager)
        val transactionDataSorterFactory = TransactionDataSorterFactory()

        var dustCalculator: DustCalculator? = null
        var transactionSizeCalculator: TransactionSizeCalculator? = null
        var transactionFeeCalculator: TransactionFeeCalculator? = null
        var transactionSender: TransactionSender? = null
        var transactionCreator: TransactionCreator? = null

        if (privateWallet != null) {
            val ecdsaInputSigner = EcdsaInputSigner(privateWallet, network)
            val schnorrInputSigner = SchnorrInputSigner(privateWallet)
            val transactionSizeCalculatorInstance = TransactionSizeCalculator()
            val dustCalculatorInstance = DustCalculator(network.dustRelayTxFee, transactionSizeCalculatorInstance)
            val recipientSetter = RecipientSetter(addressConverter, pluginManager)
            val outputSetter = OutputSetter(transactionDataSorterFactory)
            val inputSetter = InputSetter(
                unspentOutputSelector,
                publicKeyManager,
                addressConverter,
                purpose.scriptType,
                transactionSizeCalculatorInstance,
                pluginManager,
                dustCalculatorInstance,
                transactionDataSorterFactory
            )
            val lockTimeSetter = LockTimeSetter(storage)
            val signer = TransactionSigner(ecdsaInputSigner, schnorrInputSigner)
            val transactionBuilder = TransactionBuilder(recipientSetter, outputSetter, inputSetter, signer, lockTimeSetter)
            transactionFeeCalculator = TransactionFeeCalculator(recipientSetter, inputSetter, addressConverter, publicKeyManager, purpose.scriptType)
            val transactionSendTimer = TransactionSendTimer(60)
            val transactionSenderInstance = TransactionSender(transactionSyncer, peerManager, initialBlockDownload, storage, transactionSendTimer)

            dustCalculator = dustCalculatorInstance
            transactionSizeCalculator = transactionSizeCalculatorInstance
            transactionSender = transactionSenderInstance

            transactionSendTimer.listener = transactionSender

            transactionCreator = TransactionCreator(transactionBuilder, pendingTransactionProcessor, transactionSenderInstance, bloomFilterManager)
        }

        val blockHashFetcher = BlockHashFetcher(restoreKeyConverterChain, initialSyncApi, BlockHashFetcherHelper())
        val blockDiscovery = BlockDiscoveryBatch(blockHashFetcher, publicKeyFetcher, checkpoint.block.height, gapLimit)
        val apiSyncStateManager = ApiSyncStateManager(storage, network.syncableFromApi && syncMode is BitcoinCore.SyncMode.Api)
        val initialSyncer = InitialSyncer(storage, blockDiscovery, publicKeyManager, multiAccountPublicKeyFetcher)

        val syncManager = SyncManager(connectionManager, initialSyncer, peerGroup, apiSyncStateManager, blockSyncer.localDownloadedBestBlockHeight)
        initialSyncer.listener = syncManager
        connectionManager.listener = syncManager
        blockSyncer.listener = syncManager
        initialBlockDownload.listener = syncManager
        blockHashFetcher.listener = syncManager

        val bitcoinCore = BitcoinCore(
            storage,
            dataProvider,
            publicKeyManager,
            addressConverter,
            restoreKeyConverterChain,
            transactionCreator,
            transactionFeeCalculator,
            paymentAddressParser,
            syncManager,
            purpose,
            peerManager,
            dustCalculator,
            pluginManager,
            connectionManager
        )

        dataProvider.listener = bitcoinCore
        syncManager.listener = bitcoinCore

        val watchedTransactionManager = WatchedTransactionManager()
        bloomFilterManager.addBloomFilterProvider(watchedTransactionManager)
        bloomFilterManager.addBloomFilterProvider(bloomFilterProvider)
        bloomFilterManager.addBloomFilterProvider(pendingOutpointsProvider)
        bloomFilterManager.addBloomFilterProvider(irregularOutputFinder)

        bitcoinCore.watchedTransactionManager = watchedTransactionManager
        pendingTransactionProcessor.transactionListener = watchedTransactionManager
        blockTransactionProcessor.transactionListener = watchedTransactionManager

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


        val mempoolTransactions = MempoolTransactions(transactionSyncer, transactionSender)
        bitcoinCore.addPeerTaskHandler(mempoolTransactions)
        bitcoinCore.addInventoryItemsHandler(mempoolTransactions)
        bitcoinCore.addPeerGroupListener(mempoolTransactions)

        transactionSender?.let {
            bitcoinCore.addPeerSyncListener(SendTransactionsOnPeersSynced(transactionSender))
            bitcoinCore.addPeerTaskHandler(transactionSender)
        }

        transactionSizeCalculator?.let {
            bitcoinCore.prependUnspentOutputSelector(UnspentOutputSelector(transactionSizeCalculator, unspentOutputProvider))
            bitcoinCore.prependUnspentOutputSelector(UnspentOutputSelectorSingleNoChange(transactionSizeCalculator, unspentOutputProvider))
        }

        return bitcoinCore
    }
}

class BitcoinCore(
    private val storage: IStorage,
    private val dataProvider: DataProvider,
    private val publicKeyManager: IPublicKeyManager,
    private val addressConverter: AddressConverterChain,
    private val restoreKeyConverterChain: RestoreKeyConverterChain,
    private val transactionCreator: TransactionCreator?,
    private val transactionFeeCalculator: TransactionFeeCalculator?,
    private val paymentAddressParser: PaymentAddressParser,
    private val syncManager: SyncManager,
    private val purpose: Purpose,
    private var peerManager: PeerManager,
    private val dustCalculator: DustCalculator?,
    private val pluginManager: PluginManager,
    private val connectionManager: IConnectionManager
) : IKitStateListener, DataProvider.Listener {

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
    val syncState get() = syncManager.syncState

    var listener: Listener? = null

    val watchAccount: Boolean
        get() = transactionCreator == null

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

    fun onEnterForeground() {
        connectionManager.onEnterForeground()
    }

    fun onEnterBackground() {
        connectionManager.onEnterBackground()
    }

    fun transactions(fromUid: String? = null, type: TransactionFilterType? = null, limit: Int? = null): Single<List<TransactionInfo>> {
        return dataProvider.transactions(fromUid, type, limit)
    }

    fun fee(value: Long, address: String? = null, senderPay: Boolean = true, feeRate: Int, pluginData: Map<Byte, IPluginData>): Long {
        return transactionFeeCalculator?.fee(value, feeRate, senderPay, address, pluginData) ?: throw CoreError.ReadOnlyCore
    }

    fun send(
        address: String,
        value: Long,
        senderPay: Boolean = true,
        feeRate: Int,
        sortType: TransactionDataSortType,
        pluginData: Map<Byte, IPluginData>
    ): FullTransaction {
        return transactionCreator?.create(address, value, feeRate, senderPay, sortType, pluginData) ?: throw CoreError.ReadOnlyCore
    }

    fun send(
        hash: ByteArray,
        scriptType: ScriptType,
        value: Long,
        senderPay: Boolean = true,
        feeRate: Int,
        sortType: TransactionDataSortType
    ): FullTransaction {
        val address = addressConverter.convert(hash, scriptType)
        return transactionCreator?.create(address.stringValue, value, feeRate, senderPay, sortType, mapOf()) ?: throw CoreError.ReadOnlyCore
    }

    fun redeem(unspentOutput: UnspentOutput, address: String, feeRate: Int, sortType: TransactionDataSortType): FullTransaction {
        return transactionCreator?.create(unspentOutput, address, feeRate, sortType) ?: throw CoreError.ReadOnlyCore
    }

    fun receiveAddress(): String {
        return addressConverter.convert(publicKeyManager.receivePublicKey(), purpose.scriptType).stringValue
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

                val legacy = addressConverter.convert(pubKey.publicKeyHash, ScriptType.P2PKH).stringValue
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
        statusInfo["Derivation"] = purpose.description
        statusInfo["Sync State"] = syncState.toString()
        statusInfo["Last Block Height"] = lastBlockInfo?.height ?: "N/A"

        val peers = LinkedHashMap<String, Any>()
        peerManager.connected().forEachIndexed { index, peer ->

            val peerStatus = LinkedHashMap<String, Any>()
            peerStatus["Status"] = if (peer.synced) "Synced" else "Not Synced"
            peerStatus["Host"] = peer.host
            peerStatus["Best Block"] = peer.announcedLastBlockHeight
            peerStatus["User Agent"] = peer.subVersion

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
    // IKitStateManagerListener implementations
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
        return transactionFeeCalculator?.let { transactionFeeCalculator ->
            balance.spendable - transactionFeeCalculator.fee(balance.spendable, feeRate, false, address, pluginData)
        } ?: throw CoreError.ReadOnlyCore
    }

    fun minimumSpendableValue(address: String?): Int {
        // by default script type is P2PKH, since it is most used
        val scriptType = when {
            address != null -> addressConverter.convert(address).scriptType
            else -> ScriptType.P2PKH
        }

        return dustCalculator?.dust(scriptType) ?: throw CoreError.ReadOnlyCore
    }

    fun getRawTransaction(transactionHash: String): String? {
        return dataProvider.getRawTransaction(transactionHash)
    }

    fun getTransaction(hash: String): TransactionInfo? {
        return dataProvider.getTransaction(hash)
    }

    sealed class KitState {
        object Synced : KitState()
        class NotSynced(val exception: Throwable) : KitState()
        class Syncing(val progress: Double) : KitState()
        class ApiSyncing(val transactions: Int) : KitState()

        override fun equals(other: Any?) = when {
            this is Synced && other is Synced -> true
            this is NotSynced && other is NotSynced -> exception == other.exception
            this is Syncing && other is Syncing -> this.progress == other.progress
            this is ApiSyncing && other is ApiSyncing -> this.transactions == other.transactions
            else -> false
        }

        override fun toString() = when (this) {
            is Synced -> "Synced"
            is NotSynced -> "NotSynced-${this.exception.javaClass.simpleName}"
            is Syncing -> "Syncing-${(this.progress * 100).roundToInt() / 100.0}"
            is ApiSyncing -> "ApiSyncing-$transactions"
        }

        override fun hashCode(): Int {
            var result = javaClass.hashCode()
            if (this is Syncing) {
                result = 31 * result + progress.hashCode()
            }
            if (this is NotSynced) {
                result = 31 * result + exception.hashCode()
            }
            if (this is ApiSyncing) {
                result = 31 * result + transactions.hashCode()
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

    sealed class CoreError : Exception() {
        object ReadOnlyCore : CoreError()
    }


}
