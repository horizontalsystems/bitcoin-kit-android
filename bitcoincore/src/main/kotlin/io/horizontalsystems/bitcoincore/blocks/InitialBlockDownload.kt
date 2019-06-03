package io.horizontalsystems.bitcoincore.blocks

import io.horizontalsystems.bitcoincore.core.ISyncStateListener
import io.horizontalsystems.bitcoincore.models.InventoryItem
import io.horizontalsystems.bitcoincore.models.MerkleBlock
import io.horizontalsystems.bitcoincore.network.peer.*
import io.horizontalsystems.bitcoincore.network.peer.task.GetBlockHashesTask
import io.horizontalsystems.bitcoincore.network.peer.task.GetMerkleBlocksTask
import io.horizontalsystems.bitcoincore.network.peer.task.PeerTask
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.logging.Logger

class InitialBlockDownload(
        private var blockSyncer: BlockSyncer?,
        private val peerManager: PeerManager,
        private val syncStateListener: ISyncStateListener,
        private val merkleBlockExtractor: MerkleBlockExtractor)
    : IInventoryItemsHandler, IPeerTaskHandler, PeerGroup.Listener, GetMerkleBlocksTask.MerkleBlockHandler {

    val syncedPeers = CopyOnWriteArrayList<Peer>()
    private val peerSyncListeners = mutableListOf<IPeerSyncListener>()

    @Volatile
    private var syncPeer: Peer? = null
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
                    blockSyncer?.addBlockHashes(task.blockHashes)
                }
                true
            }
            is GetMerkleBlocksTask -> {
                blockSyncer?.downloadIterationCompleted()
                true
            }
            else -> false
        }
    }

    override fun handleMerkleBlock(merkleBlock: MerkleBlock) {
        val maxBlockHeight = syncPeer?.announcedLastBlockHeight ?: 0
        blockSyncer?.handleMerkleBlock(merkleBlock, maxBlockHeight)
    }

    override fun onStart() {
        syncStateListener.onSyncStart()
        blockSyncer?.prepareForDownload()
    }

    override fun onStop() {
        syncStateListener.onSyncStop()
    }

    override fun onPeerCreate(peer: Peer) {
        peer.localBestBlockHeight = blockSyncer?.localDownloadedBestBlockHeight ?: 0
    }

    override fun onPeerConnect(peer: Peer) {
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
            blockSyncer?.downloadFailed()
            assignNextSyncPeer()
        }
    }

    private fun assignNextSyncPeer() {
        peersQueue.execute {
            if (syncPeer == null) {
                val notSyncedPeers = peerManager.connected().filter { !it.synced }
                if (notSyncedPeers.isEmpty()) {
                    peerSyncListeners.forEach { it.onAllPeersSynced() }
                }

                notSyncedPeers.firstOrNull { it.ready }?.let { nonSyncedPeer ->
                    syncPeer = nonSyncedPeer
                    blockSyncer?.downloadStarted()

                    logger.info("Start syncing peer ${nonSyncedPeer.host}")

                    downloadBlockchain()
                }
            }
        }
    }

    private fun downloadBlockchain() {
        if (syncPeer?.ready != true) return

        syncPeer?.let { peer ->
            blockSyncer?.let { blockSyncer ->

                val blockHashes = blockSyncer.getBlockHashes()
                if (blockHashes.isEmpty()) {
                    peer.synced = peer.blockHashesSynced
                } else {
                    peer.addTask(GetMerkleBlocksTask(blockHashes, this, merkleBlockExtractor, minMerkleBlocks, minTransactions, minReceiveBytes))
                }

                if (!peer.blockHashesSynced) {
                    val expectedHashesMinCount = Math.max(peer.announcedLastBlockHeight - blockSyncer.localKnownBestBlockHeight, 0)
                    peer.addTask(GetBlockHashesTask(blockSyncer.getBlockLocatorHashes(peer.announcedLastBlockHeight), expectedHashesMinCount))
                }

                if (peer.synced) {
                    syncedPeers.add(peer)

                    blockSyncer.downloadCompleted()
                    syncStateListener.onSyncFinish()
                    peer.sendMempoolMessage()
                    logger.info("Peer synced ${peer.host}")
                    syncPeer = null
                    assignNextSyncPeer()
                    peerSyncListeners.forEach { it.onPeerSynced(peer) }
                }
            }
        }
    }

}
