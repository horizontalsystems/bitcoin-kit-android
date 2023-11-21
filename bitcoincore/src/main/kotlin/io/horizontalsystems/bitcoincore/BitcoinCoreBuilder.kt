package io.horizontalsystems.bitcoincore

import android.content.Context
import io.horizontalsystems.bitcoincore.apisync.blockchair.BlockchairApi
import io.horizontalsystems.bitcoincore.apisync.blockchair.BlockchairApiSyncer
import io.horizontalsystems.bitcoincore.apisync.blockchair.BlockchairLastBlockProvider
import io.horizontalsystems.bitcoincore.apisync.blockchair.BlockchairTransactionProvider
import io.horizontalsystems.bitcoincore.apisync.legacy.ApiSyncer
import io.horizontalsystems.bitcoincore.apisync.legacy.BlockHashDiscoveryBatch
import io.horizontalsystems.bitcoincore.apisync.legacy.BlockHashScanHelper
import io.horizontalsystems.bitcoincore.apisync.legacy.BlockHashScanner
import io.horizontalsystems.bitcoincore.apisync.legacy.IMultiAccountPublicKeyFetcher
import io.horizontalsystems.bitcoincore.apisync.legacy.IPublicKeyFetcher
import io.horizontalsystems.bitcoincore.apisync.legacy.MultiAccountPublicKeyFetcher
import io.horizontalsystems.bitcoincore.apisync.legacy.PublicKeyFetcher
import io.horizontalsystems.bitcoincore.apisync.legacy.WatchAddressBlockHashScanHelper
import io.horizontalsystems.bitcoincore.apisync.legacy.WatchPublicKeyFetcher
import io.horizontalsystems.bitcoincore.blocks.BlockDownload
import io.horizontalsystems.bitcoincore.blocks.BlockSyncer
import io.horizontalsystems.bitcoincore.blocks.Blockchain
import io.horizontalsystems.bitcoincore.blocks.BloomFilterLoader
import io.horizontalsystems.bitcoincore.blocks.InitialBlockDownload
import io.horizontalsystems.bitcoincore.blocks.MerkleBlockExtractor
import io.horizontalsystems.bitcoincore.blocks.validators.IBlockValidator
import io.horizontalsystems.bitcoincore.core.AccountWallet
import io.horizontalsystems.bitcoincore.core.BaseTransactionInfoConverter
import io.horizontalsystems.bitcoincore.core.DataProvider
import io.horizontalsystems.bitcoincore.core.DoubleSha256Hasher
import io.horizontalsystems.bitcoincore.core.IApiSyncer
import io.horizontalsystems.bitcoincore.core.IApiTransactionProvider
import io.horizontalsystems.bitcoincore.core.IHasher
import io.horizontalsystems.bitcoincore.core.IInitialDownload
import io.horizontalsystems.bitcoincore.core.IPlugin
import io.horizontalsystems.bitcoincore.core.IPrivateWallet
import io.horizontalsystems.bitcoincore.core.IPublicKeyManager
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.core.ITransactionInfoConverter
import io.horizontalsystems.bitcoincore.core.PluginManager
import io.horizontalsystems.bitcoincore.core.TransactionDataSorterFactory
import io.horizontalsystems.bitcoincore.core.TransactionInfoConverter
import io.horizontalsystems.bitcoincore.core.Wallet
import io.horizontalsystems.bitcoincore.core.WatchAccountWallet
import io.horizontalsystems.bitcoincore.core.scriptType
import io.horizontalsystems.bitcoincore.managers.AccountPublicKeyManager
import io.horizontalsystems.bitcoincore.managers.ApiSyncStateManager
import io.horizontalsystems.bitcoincore.managers.BloomFilterManager
import io.horizontalsystems.bitcoincore.managers.ConnectionManager
import io.horizontalsystems.bitcoincore.managers.IBloomFilterProvider
import io.horizontalsystems.bitcoincore.managers.IrregularOutputFinder
import io.horizontalsystems.bitcoincore.managers.PendingOutpointsProvider
import io.horizontalsystems.bitcoincore.managers.PublicKeyManager
import io.horizontalsystems.bitcoincore.managers.RestoreKeyConverterChain
import io.horizontalsystems.bitcoincore.managers.SyncManager
import io.horizontalsystems.bitcoincore.managers.UnspentOutputProvider
import io.horizontalsystems.bitcoincore.managers.UnspentOutputSelector
import io.horizontalsystems.bitcoincore.managers.UnspentOutputSelectorChain
import io.horizontalsystems.bitcoincore.managers.UnspentOutputSelectorSingleNoChange
import io.horizontalsystems.bitcoincore.models.Checkpoint
import io.horizontalsystems.bitcoincore.models.WatchAddressPublicKey
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.network.messages.AddrMessageParser
import io.horizontalsystems.bitcoincore.network.messages.FilterLoadMessageSerializer
import io.horizontalsystems.bitcoincore.network.messages.GetBlocksMessageSerializer
import io.horizontalsystems.bitcoincore.network.messages.GetDataMessageParser
import io.horizontalsystems.bitcoincore.network.messages.GetDataMessageSerializer
import io.horizontalsystems.bitcoincore.network.messages.InvMessageParser
import io.horizontalsystems.bitcoincore.network.messages.InvMessageSerializer
import io.horizontalsystems.bitcoincore.network.messages.MempoolMessageSerializer
import io.horizontalsystems.bitcoincore.network.messages.MerkleBlockMessageParser
import io.horizontalsystems.bitcoincore.network.messages.NetworkMessageParser
import io.horizontalsystems.bitcoincore.network.messages.NetworkMessageSerializer
import io.horizontalsystems.bitcoincore.network.messages.PingMessageParser
import io.horizontalsystems.bitcoincore.network.messages.PingMessageSerializer
import io.horizontalsystems.bitcoincore.network.messages.PongMessageParser
import io.horizontalsystems.bitcoincore.network.messages.PongMessageSerializer
import io.horizontalsystems.bitcoincore.network.messages.RejectMessageParser
import io.horizontalsystems.bitcoincore.network.messages.TransactionMessageParser
import io.horizontalsystems.bitcoincore.network.messages.TransactionMessageSerializer
import io.horizontalsystems.bitcoincore.network.messages.VerAckMessageParser
import io.horizontalsystems.bitcoincore.network.messages.VerAckMessageSerializer
import io.horizontalsystems.bitcoincore.network.messages.VersionMessageParser
import io.horizontalsystems.bitcoincore.network.messages.VersionMessageSerializer
import io.horizontalsystems.bitcoincore.network.peer.MempoolTransactions
import io.horizontalsystems.bitcoincore.network.peer.PeerAddressManager
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup
import io.horizontalsystems.bitcoincore.network.peer.PeerManager
import io.horizontalsystems.bitcoincore.serializers.BlockHeaderParser
import io.horizontalsystems.bitcoincore.transactions.BlockTransactionProcessor
import io.horizontalsystems.bitcoincore.transactions.PendingTransactionProcessor
import io.horizontalsystems.bitcoincore.transactions.SendTransactionsOnPeersSynced
import io.horizontalsystems.bitcoincore.transactions.TransactionConflictsResolver
import io.horizontalsystems.bitcoincore.transactions.TransactionCreator
import io.horizontalsystems.bitcoincore.transactions.TransactionFeeCalculator
import io.horizontalsystems.bitcoincore.transactions.TransactionInvalidator
import io.horizontalsystems.bitcoincore.transactions.TransactionSendTimer
import io.horizontalsystems.bitcoincore.transactions.TransactionSender
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.TransactionSyncer
import io.horizontalsystems.bitcoincore.transactions.builder.EcdsaInputSigner
import io.horizontalsystems.bitcoincore.transactions.builder.InputSetter
import io.horizontalsystems.bitcoincore.transactions.builder.LockTimeSetter
import io.horizontalsystems.bitcoincore.transactions.builder.OutputSetter
import io.horizontalsystems.bitcoincore.transactions.builder.RecipientSetter
import io.horizontalsystems.bitcoincore.transactions.builder.SchnorrInputSigner
import io.horizontalsystems.bitcoincore.transactions.builder.TransactionBuilder
import io.horizontalsystems.bitcoincore.transactions.builder.TransactionSigner
import io.horizontalsystems.bitcoincore.transactions.extractors.MyOutputsCache
import io.horizontalsystems.bitcoincore.transactions.extractors.TransactionExtractor
import io.horizontalsystems.bitcoincore.transactions.extractors.TransactionMetadataExtractor
import io.horizontalsystems.bitcoincore.transactions.extractors.TransactionOutputProvider
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.AddressConverterChain
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.PaymentAddressParser
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.HDWalletAccount
import io.horizontalsystems.hdwalletkit.HDWalletAccountWatch

class BitcoinCoreBuilder {

    val addressConverter = AddressConverterChain()

    // required parameters
    private var context: Context? = null
    private var extendedKey: HDExtendedKey? = null
    private var watchAddressPublicKey: WatchAddressPublicKey? = null
    private var purpose: HDWallet.Purpose? = null
    private var network: Network? = null
    private var paymentAddressParser: PaymentAddressParser? = null
    private var storage: IStorage? = null
    private var apiTransactionProvider: IApiTransactionProvider? = null
    private var blockHeaderHasher: IHasher? = null
    private var transactionInfoConverter: ITransactionInfoConverter? = null
    private var blockValidator: IBlockValidator? = null
    private var checkpoint: Checkpoint? = null
    private var apiSyncStateManager: ApiSyncStateManager? = null

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

    fun setExtendedKey(extendedKey: HDExtendedKey?): BitcoinCoreBuilder {
        this.extendedKey = extendedKey
        return this
    }

    fun setWatchAddressPublicKey(publicKey: WatchAddressPublicKey?): BitcoinCoreBuilder {
        this.watchAddressPublicKey = publicKey
        return this
    }

    fun setPurpose(purpose: HDWallet.Purpose): BitcoinCoreBuilder {
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

    fun setApiTransactionProvider(apiTransactionProvider: IApiTransactionProvider?): BitcoinCoreBuilder {
        this.apiTransactionProvider = apiTransactionProvider
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

    fun setCheckpoint(checkpoint: Checkpoint): BitcoinCoreBuilder {
        this.checkpoint = checkpoint
        return this
    }

    fun setApiSyncStateManager(apiSyncStateManager: ApiSyncStateManager): BitcoinCoreBuilder {
        this.apiSyncStateManager = apiSyncStateManager
        return this
    }

    fun build(): BitcoinCore {
        val context = checkNotNull(this.context)
        val extendedKey = this.extendedKey
        val watchAddressPublicKey = this.watchAddressPublicKey
        val purpose = checkNotNull(this.purpose)
        val network = checkNotNull(this.network)
        val paymentAddressParser = checkNotNull(this.paymentAddressParser)
        val storage = checkNotNull(this.storage)
        val apiTransactionProvider = checkNotNull(this.apiTransactionProvider)
        val checkpoint = checkNotNull(this.checkpoint)
        val apiSyncStateManager = checkNotNull(this.apiSyncStateManager)
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

        if (watchAddressPublicKey != null) {
            storage.savePublicKeys(listOf(watchAddressPublicKey))

            WatchAddressPublicKeyManager(watchAddressPublicKey, restoreKeyConverterChain).let {
                publicKeyFetcher = it
                publicKeyManager = it
                bloomFilterProvider = it
            }
        } else if (extendedKey != null) {
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
        } else {
            throw IllegalStateException("Both extendedKey and watchAddressPublicKey are NULL!")
        }

        val pendingOutpointsProvider = PendingOutpointsProvider(storage)

        val additionalScriptTypes = if (watchAddressPublicKey != null) listOf(ScriptType.P2PKH) else emptyList()
        val irregularOutputFinder = IrregularOutputFinder(storage, additionalScriptTypes)
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
        val blockSyncer = BlockSyncer(storage, blockchain, blockTransactionProcessor, publicKeyManager, checkpoint)


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

        val blockHashScanHelper = if (watchAddressPublicKey == null) BlockHashScanHelper() else WatchAddressBlockHashScanHelper()
        val blockHashScanner = BlockHashScanner(restoreKeyConverterChain, apiTransactionProvider, blockHashScanHelper)

        val apiSyncer: IApiSyncer
        val initialDownload: IInitialDownload
        val merkleBlockExtractor = MerkleBlockExtractor(network.maxBlockSize)

        when (val syncMode = syncMode) {
            is BitcoinCore.SyncMode.Blockchair -> {
                val blockchairApi = if (apiTransactionProvider is BlockchairTransactionProvider) {
                    apiTransactionProvider.blockchairApi
                } else {
                    BlockchairApi(syncMode.key, network.blockchairChainId)
                }
                val lastBlockProvider = BlockchairLastBlockProvider(blockchairApi)
                apiSyncer = BlockchairApiSyncer(
                    storage,
                    restoreKeyConverterChain,
                    apiTransactionProvider,
                    lastBlockProvider,
                    publicKeyManager,
                    blockchain,
                    apiSyncStateManager
                )
                initialDownload = BlockDownload(blockSyncer, peerManager, merkleBlockExtractor)
            }

            else -> {
                val blockDiscovery = BlockHashDiscoveryBatch(blockHashScanner, publicKeyFetcher, checkpoint.block.height, gapLimit)
                apiSyncer = ApiSyncer(
                    storage,
                    blockDiscovery,
                    publicKeyManager,
                    multiAccountPublicKeyFetcher,
                    apiSyncStateManager
                )
                initialDownload = InitialBlockDownload(blockSyncer, peerManager, merkleBlockExtractor)
            }
        }

        val syncManager = SyncManager(connectionManager, apiSyncer, peerGroup, storage, syncMode, blockSyncer.localDownloadedBestBlockHeight)
        apiSyncer.listener = syncManager
        connectionManager.listener = syncManager
        blockSyncer.listener = syncManager
        initialDownload.listener = syncManager
        blockHashScanner.listener = syncManager

        val unspentOutputSelector = UnspentOutputSelectorChain()
        val pendingTransactionSyncer = TransactionSyncer(storage, pendingTransactionProcessor, invalidator, publicKeyManager)
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
            val transactionSenderInstance = TransactionSender(
                pendingTransactionSyncer,
                peerManager,
                initialDownload,
                storage,
                transactionSendTimer
            )

            dustCalculator = dustCalculatorInstance
            transactionSizeCalculator = transactionSizeCalculatorInstance
            transactionSender = transactionSenderInstance

            transactionSendTimer.listener = transactionSender

            transactionCreator = TransactionCreator(transactionBuilder, pendingTransactionProcessor, transactionSenderInstance, bloomFilterManager)
        }

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
        bitcoinCore.transactionSyncer = pendingTransactionSyncer
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
        bitcoinCore.initialDownload = initialDownload
        bitcoinCore.addPeerTaskHandler(initialDownload)
        bitcoinCore.addInventoryItemsHandler(initialDownload)
        bitcoinCore.addPeerGroupListener(initialDownload)


        val mempoolTransactions = MempoolTransactions(pendingTransactionSyncer, transactionSender)
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
