package io.horizontalsystems.dashkit

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCore.SyncMode
import io.horizontalsystems.bitcoincore.BitcoinCoreBuilder
import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorChain
import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorSet
import io.horizontalsystems.bitcoincore.blocks.validators.ProofOfWorkValidator
import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.managers.*
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.CoreDatabase
import io.horizontalsystems.bitcoincore.storage.Storage
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.MerkleBranch
import io.horizontalsystems.bitcoincore.utils.PaymentAddressParser
import io.horizontalsystems.dashkit.core.DashTransactionInfoConverter
import io.horizontalsystems.dashkit.core.SingleSha256Hasher
import io.horizontalsystems.dashkit.instantsend.*
import io.horizontalsystems.dashkit.instantsend.instantsendlock.InstantSendLockHandler
import io.horizontalsystems.dashkit.instantsend.instantsendlock.InstantSendLockManager
import io.horizontalsystems.dashkit.instantsend.transactionlockvote.TransactionLockVoteHandler
import io.horizontalsystems.dashkit.instantsend.transactionlockvote.TransactionLockVoteManager
import io.horizontalsystems.dashkit.managers.*
import io.horizontalsystems.dashkit.masternodelist.*
import io.horizontalsystems.dashkit.messages.*
import io.horizontalsystems.dashkit.models.CoinbaseTransactionSerializer
import io.horizontalsystems.dashkit.models.DashTransactionInfo
import io.horizontalsystems.dashkit.models.InstantTransactionState
import io.horizontalsystems.dashkit.storage.DashKitDatabase
import io.horizontalsystems.dashkit.storage.DashStorage
import io.horizontalsystems.dashkit.tasks.PeerTaskFactory
import io.horizontalsystems.dashkit.validators.DarkGravityWaveTestnetValidator
import io.horizontalsystems.dashkit.validators.DarkGravityWaveValidator
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.Single

class DashKit : AbstractKit, IInstantTransactionDelegate, BitcoinCore.Listener {
    enum class NetworkType {
        MainNet,
        TestNet
    }

    interface Listener {
        fun onTransactionsUpdate(inserted: List<DashTransactionInfo>, updated: List<DashTransactionInfo>)
        fun onTransactionsDelete(hashes: List<String>)
        fun onBalanceUpdate(balance: BalanceInfo)
        fun onLastBlockInfoUpdate(blockInfo: BlockInfo)
        fun onKitStateUpdate(state: BitcoinCore.KitState)
    }

    var listener: Listener? = null

    override var bitcoinCore: BitcoinCore
    override var network: Network

    private val dashStorage: DashStorage
    private val instantSend: InstantSend
    private val dashTransactionInfoConverter: DashTransactionInfoConverter

    constructor(
            context: Context,
            words: List<String>,
            passphrase: String,
            walletId: String,
            networkType: NetworkType = NetworkType.MainNet,
            peerSize: Int = 10,
            syncMode: SyncMode = SyncMode.Api(),
            confirmationsThreshold: Int = 6
    ) : this(context, Mnemonic().toSeed(words, passphrase), walletId, networkType, peerSize, syncMode, confirmationsThreshold)

    constructor(
            context: Context,
            seed: ByteArray,
            walletId: String,
            networkType: NetworkType = NetworkType.MainNet,
            peerSize: Int = 10,
            syncMode: SyncMode = SyncMode.Api(),
            confirmationsThreshold: Int = 6
    ) {
        val coreDatabase = CoreDatabase.getInstance(context, getDatabaseNameCore(networkType, walletId, syncMode))
        val dashDatabase = DashKitDatabase.getInstance(context, getDatabaseName(networkType, walletId, syncMode))
        val initialSyncUrl: String

        val coreStorage = Storage(coreDatabase)
        dashStorage = DashStorage(dashDatabase, coreStorage)

        network = when (networkType) {
            NetworkType.MainNet -> {
                initialSyncUrl = "https://dash.horizontalsystems.xyz/apg"
                MainNetDash()
            }
            NetworkType.TestNet -> {
                initialSyncUrl = "http://dash-testnet.horizontalsystems.xyz/apg"
                TestNetDash()
            }
        }

        val paymentAddressParser = PaymentAddressParser("dash", removeScheme = true)
        val instantTransactionManager = InstantTransactionManager(dashStorage, InstantSendFactory(), InstantTransactionState())
        val initialSyncApi = InsightApi(initialSyncUrl)

        dashTransactionInfoConverter = DashTransactionInfoConverter(instantTransactionManager)

        val blockHelper = BlockValidatorHelper(coreStorage)

        val blockValidatorSet = BlockValidatorSet()
        blockValidatorSet.addBlockValidator(ProofOfWorkValidator())

        val blockValidatorChain = BlockValidatorChain()

        if (network is MainNetDash) {
            blockValidatorChain.add(DarkGravityWaveValidator(blockHelper, heightInterval, targetTimespan, maxTargetBits, 68589))
        } else {
            blockValidatorChain.add(DarkGravityWaveTestnetValidator(targetSpacing, targetTimespan, maxTargetBits, 4002))
            blockValidatorChain.add(DarkGravityWaveValidator(blockHelper, heightInterval, targetTimespan, maxTargetBits, 4002))
        }

        blockValidatorSet.addBlockValidator(blockValidatorChain)

        bitcoinCore = BitcoinCoreBuilder()
                .setContext(context)
                .setSeed(seed)
                .setNetwork(network)
                .setPaymentAddressParser(paymentAddressParser)
                .setPeerSize(peerSize)
                .setSyncMode(syncMode)
                .setConfirmationThreshold(confirmationsThreshold)
                .setStorage(coreStorage)
                .setBlockHeaderHasher(X11Hasher())
                .setInitialSyncApi(initialSyncApi)
                .setTransactionInfoConverter(dashTransactionInfoConverter)
                .setBlockValidator(blockValidatorSet)
                .build()

        bitcoinCore.listener = this

        //  extending bitcoinCore



        bitcoinCore.addMessageParser(MasternodeListDiffMessageParser())
                .addMessageParser(TransactionLockMessageParser())
                .addMessageParser(TransactionLockVoteMessageParser())
                .addMessageParser(ISLockMessageParser())

        bitcoinCore.addMessageSerializer(GetMasternodeListDiffMessageSerializer())

        val merkleRootHasher = MerkleRootHasher()
        val merkleRootCreator = MerkleRootCreator(merkleRootHasher)
        val masternodeListMerkleRootCalculator = MasternodeListMerkleRootCalculator(merkleRootCreator)
        val masternodeCbTxHasher = MasternodeCbTxHasher(CoinbaseTransactionSerializer(), merkleRootHasher)

        val quorumListManager = QuorumListManager(dashStorage, QuorumListMerkleRootCalculator(merkleRootCreator), QuorumSortedList())
        val masternodeListManager = MasternodeListManager(dashStorage, masternodeListMerkleRootCalculator, masternodeCbTxHasher, MerkleBranch(), MasternodeSortedList(), quorumListManager)
        val masternodeSyncer = MasternodeListSyncer(bitcoinCore, PeerTaskFactory(), masternodeListManager, bitcoinCore.initialBlockDownload)

        bitcoinCore.addPeerTaskHandler(masternodeSyncer)
        bitcoinCore.addPeerSyncListener(masternodeSyncer)
        bitcoinCore.addPeerGroupListener(masternodeSyncer)

        val base58AddressConverter = Base58AddressConverter(network.addressVersion, network.addressScriptVersion)
        bitcoinCore.addRestoreKeyConverter(Bip44RestoreKeyConverter(base58AddressConverter))

        val singleHasher = SingleSha256Hasher()
        val bls = BLS()
        val transactionLockVoteValidator = TransactionLockVoteValidator(dashStorage, singleHasher, bls)
        val instantSendLockValidator = InstantSendLockValidator(quorumListManager, bls)

        val transactionLockVoteManager = TransactionLockVoteManager(transactionLockVoteValidator)
        val instantSendLockManager = InstantSendLockManager(instantSendLockValidator)

        val instantSendLockHandler = InstantSendLockHandler(instantTransactionManager, instantSendLockManager)
        instantSendLockHandler.delegate = this
        val transactionLockVoteHandler = TransactionLockVoteHandler(instantTransactionManager, transactionLockVoteManager)
        transactionLockVoteHandler.delegate = this

        val instantSend = InstantSend(bitcoinCore.transactionSyncer, transactionLockVoteHandler, instantSendLockHandler)
        this.instantSend = instantSend

        bitcoinCore.addInventoryItemsHandler(instantSend)
        bitcoinCore.addPeerTaskHandler(instantSend)

        val calculator = TransactionSizeCalculator()
        val confirmedUnspentOutputProvider = ConfirmedUnspentOutputProvider(coreStorage, confirmationsThreshold)
        bitcoinCore.prependUnspentOutputSelector(UnspentOutputSelector(calculator, confirmedUnspentOutputProvider))
        bitcoinCore.prependUnspentOutputSelector(UnspentOutputSelectorSingleNoChange(calculator, confirmedUnspentOutputProvider))
    }

    fun dashTransactions(fromUid: String? = null, limit: Int? = null): Single<List<DashTransactionInfo>> {
        return transactions(fromUid, limit).map {
            it.mapNotNull { it as? DashTransactionInfo }
        }
    }

    fun getDashTransaction(hash: String): DashTransactionInfo? {
        return getTransaction(hash) as? DashTransactionInfo
    }

    // BitcoinCore.Listener
    override fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>) {
        // check for all new transactions if it's has instant lock
        inserted.map { it.transactionHash.hexToByteArray().reversedArray() }.forEach {
            instantSend.handle(it)
        }

        listener?.onTransactionsUpdate(inserted.mapNotNull { it as? DashTransactionInfo }, updated.mapNotNull { it as? DashTransactionInfo })
    }

    override fun onTransactionsDelete(hashes: List<String>) {
        listener?.onTransactionsDelete(hashes)
    }

    override fun onBalanceUpdate(balance: BalanceInfo) {
        listener?.onBalanceUpdate(balance)
    }

    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        listener?.onLastBlockInfoUpdate(blockInfo)
    }

    override fun onKitStateUpdate(state: BitcoinCore.KitState) {
        listener?.onKitStateUpdate(state)
    }

    // IInstantTransactionDelegate
    override fun onUpdateInstant(transactionHash: ByteArray) {
        val transaction = dashStorage.getFullTransactionInfo(transactionHash) ?: return
        val transactionInfo = dashTransactionInfoConverter.transactionInfo(transaction)

        bitcoinCore.listenerExecutor.execute {
            listener?.onTransactionsUpdate(listOf(), listOf(transactionInfo))
        }
    }

    companion object {
        const val maxTargetBits: Long = 0x1e0fffff

        const val targetSpacing = 150             // 2.5 min. for mining 1 Block
        const val targetTimespan = 3600L          // 1 hour for 24 blocks
        const val heightInterval = targetTimespan / targetSpacing

        private fun getDatabaseNameCore(networkType: NetworkType, walletId: String, syncMode: SyncMode) =
                "${getDatabaseName(networkType, walletId, syncMode)}-core"

        private fun getDatabaseName(networkType: NetworkType, walletId: String, syncMode: SyncMode) =
                "Dash-${networkType.name}-$walletId-${syncMode.javaClass.simpleName}"

        fun clear(context: Context, networkType: NetworkType, walletId: String) {
            for (syncMode in listOf(SyncMode.Api(), SyncMode.Full(), SyncMode.NewWallet())) {
                try {
                    SQLiteDatabase.deleteDatabase(context.getDatabasePath(getDatabaseNameCore(networkType, walletId, syncMode)))
                    SQLiteDatabase.deleteDatabase(context.getDatabasePath(getDatabaseName(networkType, walletId, syncMode)))
                } catch (ex: Exception) {
                    continue
                }
            }
        }
    }

}
