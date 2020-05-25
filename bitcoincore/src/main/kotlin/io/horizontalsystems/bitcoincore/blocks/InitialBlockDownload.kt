package io.horizontalsystems.bitcoincore.blocks

import io.horizontalsystems.bitcoincore.core.IBlockSyncListener
import io.horizontalsystems.bitcoincore.models.InventoryItem
import io.horizontalsystems.bitcoincore.models.MerkleBlock
import io.horizontalsystems.bitcoincore.network.peer.*
import io.horizontalsystems.bitcoincore.network.peer.task.GetBlockHashesTask
import io.horizontalsystems.bitcoincore.network.peer.task.GetMerkleBlocksTask
import io.horizontalsystems.bitcoincore.network.peer.task.PeerTask
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.logging.Logger
import kotlin.math.max

class InitialBlockDownload(
        private var blockSyncer: BlockSyncer,
        private val peerManager: PeerManager,
        private val merkleBlockExtractor: MerkleBlockExtractor)
    : IInventoryItemsHandler, IPeerTaskHandler, PeerGroup.Listener, GetMerkleBlocksTask.MerkleBlockHandler {

    var listener: IBlockSyncListener? = null
    val syncedPeers = CopyOnWriteArrayList<Peer>()
    private val peerSyncListeners = mutableListOf<IPeerSyncListener>()
    private val peerSwitchMinimumRatio = 1.5

    @Volatile
    var syncPeer: Peer? = null
    private var selectNewPeer = false
    private val peersQueue = Executors.newSingleThreadExecutor()
    private val logger = Logger.getLogger("IBD")

    private var minMerkleBlocks = 500.0
    private var minTransactions = 50_000.0
    private var minReceiveBytes = 100_000.0
    private var slowPeersDisconnected = 0

    fun addPeerSyncListener(peerSyncListener: IPeerSyncListener) {
        peerSyncListeners.add(peerSyncListener)
    }

    override fun handleInventoryItems(peer: Peer, inventoryItems: List<InventoryItem>) {
        if (peer.synced && inventoryItems.any { it.type == InventoryItem.MSG_BLOCK }) {
            peer.synced = false
            peer.blockHashesSynced = false
            syncedPeers.remove(peer)

            assignNextSyncPeer()
        }
    }

    override fun handleCompletedTask(peer: Peer, task: PeerTask): Boolean {
        return when (task) {
            is GetBlockHashesTask -> {
                if (task.blockHashes.isEmpty()) {
                    peer.blockHashesSynced = true
                } else {
                    blockSyncer.addBlockHashes(task.blockHashes)
                }
                true
            }
            is GetMerkleBlocksTask -> {
                blockSyncer.downloadIterationCompleted()
                true
            }
            else -> false
        }
    }

    override fun handleMerkleBlock(merkleBlock: MerkleBlock) {
        val maxBlockHeight = syncPeer?.announcedLastBlockHeight ?: 0
        blockSyncer.handleMerkleBlock(merkleBlock, maxBlockHeight)
    }

    override fun onStart() {
        blockSyncer.prepareForDownload()
    }

    override fun onStop() = Unit

    override fun onPeerCreate(peer: Peer) {
        peer.localBestBlockHeight = blockSyncer.localDownloadedBestBlockHeight
    }

    override fun onPeerConnect(peer: Peer) {
        syncPeer?.let {
            if (it.connectionTime > peer.connectionTime * peerSwitchMinimumRatio) {
                selectNewPeer = true
            }
        }
        assignNextSyncPeer()
    }

    override fun onPeerReady(peer: Peer) {
        if (peer == syncPeer) {
            downloadBlockchain()
        }
    }

    override fun onPeerDisconnect(peer: Peer, e: Exception?) {
        if (e is GetMerkleBlocksTask.PeerTooSlow) {
            slowPeersDisconnected += 1

            if (slowPeersDisconnected >= 3) {
                slowPeersDisconnected = 0
                minMerkleBlocks /= 3
                minTransactions /= 3
                minReceiveBytes /= 3
            }
        }

        syncedPeers.remove(peer)

        if (peer == syncPeer) {
            syncPeer = null
            blockSyncer.downloadFailed()
            assignNextSyncPeer()
        }
    }

    private fun assignNextSyncPeer() {
        peersQueue.execute {
            if (syncPeer == null) {
                val notSyncedPeers = peerManager.sorted().filter { !it.synced }
                if (notSyncedPeers.isEmpty()) {
                    peerSyncListeners.forEach { it.onAllPeersSynced() }
                }

                notSyncedPeers.firstOrNull { it.ready }?.let { nonSyncedPeer ->
                    syncPeer = nonSyncedPeer
                    blockSyncer.downloadStarted()

                    logger.info("Start syncing peer ${nonSyncedPeer.host}")

                    downloadBlockchain()
                }
            }
        }
    }

    private fun downloadBlockchain() {
        syncPeer?.let { peer ->
            if (!peer.ready) return

            if (selectNewPeer) {
                selectNewPeer = false
                blockSyncer.downloadCompleted()
                syncPeer = null
                assignNextSyncPeer()
                return
            }

            val blockHashes = blockSyncer.getBlockHashes()
            if (blockHashes.isEmpty()) {
                peer.synced = peer.blockHashesSynced
            } else {
                peer.addTask(GetMerkleBlocksTask(blockHashes, this, merkleBlockExtractor, minMerkleBlocks, minTransactions, minReceiveBytes))
            }

            if (!peer.blockHashesSynced) {
                val expectedHashesMinCount = max(peer.announcedLastBlockHeight - blockSyncer.localKnownBestBlockHeight, 0)
                peer.addTask(GetBlockHashesTask(blockSyncer.getBlockLocatorHashes(peer.announcedLastBlockHeight), expectedHashesMinCount))
            }

            if (peer.synced) {
                syncedPeers.add(peer)

                blockSyncer.downloadCompleted()
                peer.sendMempoolMessage()
                logger.info("Peer synced ${peer.host}")
                syncPeer = null
                assignNextSyncPeer()
                peerSyncListeners.forEach { it.onPeerSynced(peer) }

                // Some peers fail to send InventoryMessage within expected time
                // and become 'synced' in InitialBlockDownload without sending all of their blocks.
                // In such case, we assume not all blocks are downloaded
                if (blockSyncer.localDownloadedBestBlockHeight >= peer.announcedLastBlockHeight) {
                    listener?.onBlockSyncFinished()
                }
            }
        }
    }

}
