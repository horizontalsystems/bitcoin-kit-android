package io.horizontalsystems.bitcoinkit.dash

import android.arch.persistence.room.Room
import android.content.Context
import io.horizontalsystems.bitcoinkit.AbstractKit
import io.horizontalsystems.bitcoinkit.BitcoinCore
import io.horizontalsystems.bitcoinkit.BitcoinCoreBuilder
import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import io.horizontalsystems.bitcoinkit.crypto.CompactBits
import io.horizontalsystems.bitcoinkit.dash.managers.MasternodeListManager
import io.horizontalsystems.bitcoinkit.dash.managers.MasternodeListSyncer
import io.horizontalsystems.bitcoinkit.dash.managers.MasternodeSortedList
import io.horizontalsystems.bitcoinkit.dash.masternodelist.MasternodeCbTxHasher
import io.horizontalsystems.bitcoinkit.dash.masternodelist.MasternodeListMerkleRootCalculator
import io.horizontalsystems.bitcoinkit.dash.masternodelist.MerkleRootCreator
import io.horizontalsystems.bitcoinkit.dash.masternodelist.MerkleRootHasher
import io.horizontalsystems.bitcoinkit.dash.messages.GetMasternodeListDiffMessageSerializer
import io.horizontalsystems.bitcoinkit.dash.messages.MasternodeListDiffMessageParser
import io.horizontalsystems.bitcoinkit.dash.messages.TransactionLockMessageParser
import io.horizontalsystems.bitcoinkit.dash.messages.TransactionLockVoteMessageParser
import io.horizontalsystems.bitcoinkit.dash.models.CoinbaseTransactionSerializer
import io.horizontalsystems.bitcoinkit.dash.models.MasternodeSerializer
import io.horizontalsystems.bitcoinkit.dash.storage.DashKitDatabase
import io.horizontalsystems.bitcoinkit.dash.storage.DashStorage
import io.horizontalsystems.bitcoinkit.dash.tasks.PeerTaskFactory
import io.horizontalsystems.bitcoinkit.dash.validators.DarkGravityWaveValidator
import io.horizontalsystems.bitcoinkit.managers.BitcoinAddressSelector
import io.horizontalsystems.bitcoinkit.models.BlockInfo
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.bitcoinkit.utils.MerkleBranch
import io.horizontalsystems.bitcoinkit.utils.PaymentAddressParser
import io.horizontalsystems.hdwalletkit.Mnemonic

class DashKit : AbstractKit, BitcoinCore.Listener {
    enum class NetworkType {
        MainNet,
        TestNet
    }

    interface Listener : BitcoinCore.Listener

    override var bitcoinCore: BitcoinCore
    override var network: Network

    var listener: Listener? = null
        set(value) {
            field = value
            value?.let { bitcoinCore.addListener(it) }
        }

    private val storage: DashStorage
    private var masterNodeSyncer: MasternodeListSyncer? = null

    constructor(context: Context, words: List<String>, walletId: String, networkType: NetworkType = NetworkType.MainNet, peerSize: Int = 10, newWallet: Boolean = false, confirmationsThreshold: Int = 6) :
            this(context, Mnemonic().toSeed(words), walletId, networkType, peerSize, newWallet, confirmationsThreshold)

    constructor(context: Context, seed: ByteArray, walletId: String, networkType: NetworkType = NetworkType.MainNet, peerSize: Int = 10, newWallet: Boolean = false, confirmationsThreshold: Int = 6) {
        val databaseName = "${this.javaClass.simpleName}-${networkType.name}-$walletId"

        val database = Room.databaseBuilder(context, DashKitDatabase::class.java, databaseName)
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .addMigrations()
                .build()

        storage = DashStorage(database)

        network = when (networkType) {
            NetworkType.MainNet -> MainNetDash()
            NetworkType.TestNet -> TestNetDash()
        }

        val paymentAddressParser = PaymentAddressParser("bitcoin", removeScheme = true)

        val addressSelector = BitcoinAddressSelector()

        bitcoinCore = BitcoinCoreBuilder()
                .setContext(context)
                .setSeed(seed)
                .setNetwork(network)
                .setPaymentAddressParser(paymentAddressParser)
                .setAddressSelector(addressSelector)
                .setApiFeeRateCoinCode("DASH")
                .setPeerSize(peerSize)
                .setNewWallet(newWallet)
                .setConfirmationThreshold(confirmationsThreshold)
                .setStorage(storage)
                .setBlockHeaderHasher(X11Hasher())
                .build()

        // extending bitcoinCore

        bitcoinCore.addListener(this)
        bitcoinCore.addBlockValidator(DarkGravityWaveValidator(storage, heightInterval, targetTimespan, maxTargetBits))

        bitcoinCore.addMessageParser(MasternodeListDiffMessageParser())
        bitcoinCore.addMessageParser(TransactionLockMessageParser())
        bitcoinCore.addMessageParser(TransactionLockVoteMessageParser())

        bitcoinCore.addMessageSerializer(GetMasternodeListDiffMessageSerializer())

        val merkleRootHasher = MerkleRootHasher()
        val merkleRootCreator = MerkleRootCreator(merkleRootHasher)
        val masternodeListMerkleRootCalculator = MasternodeListMerkleRootCalculator(MasternodeSerializer(), merkleRootHasher, merkleRootCreator)
        val masternodeCbTxHasher = MasternodeCbTxHasher(CoinbaseTransactionSerializer(), merkleRootHasher)

        val masternodeListManager = MasternodeListManager(storage, masternodeListMerkleRootCalculator, masternodeCbTxHasher, MerkleBranch(), MasternodeSortedList())
        val masterNodeSyncer = MasternodeListSyncer(bitcoinCore.peerGroup, PeerTaskFactory(), masternodeListManager)
        bitcoinCore.addPeerTaskHandler(masterNodeSyncer)

        this.masterNodeSyncer = masterNodeSyncer

        val instantSend = InstantSend(bitcoinCore.transactionSyncer)
        bitcoinCore.addInventoryItemsHandler(instantSend)
        bitcoinCore.addPeerTaskHandler(instantSend)
    }

    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        if (bitcoinCore.syncState == BitcoinCore.KitState.Synced) {
            masterNodeSyncer?.sync(blockInfo.headerHash.hexStringToByteArray().reversedArray())
        }
    }

    override fun onKitStateUpdate(state: BitcoinCore.KitState) {
        if (state == BitcoinCore.KitState.Synced) {
            bitcoinCore.lastBlockInfo?.let {
                masterNodeSyncer?.sync(it.headerHash.hexStringToByteArray().reversedArray())
            }
        }
    }

    companion object {
        val maxTargetBits = CompactBits.decode(0x1e0fffff)
        val targetTimespan = 3600L     // 1 hour for 24 blocks
        val targetSpacing = 150        // 2.5 min. for mining 1 Block
        val heightInterval = targetTimespan / targetSpacing
    }
}
