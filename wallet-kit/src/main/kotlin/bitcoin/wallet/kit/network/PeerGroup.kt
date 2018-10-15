package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.crypto.BloomFilter
import bitcoin.wallet.kit.models.*
import android.util.Log
import bitcoin.wallet.kit.blocks.BlockSyncer
import bitcoin.wallet.kit.exceptions.InvalidMerkleBlockException
import bitcoin.wallet.kit.managers.BloomFilterManager
import bitcoin.wallet.kit.models.InventoryItem
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.network.PeerTask.GetBlockHashesTask
import bitcoin.wallet.kit.network.PeerTask.GetMerkleBlocksTask
import bitcoin.wallet.kit.network.PeerTask.PeerTask
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.logging.Logger

class PeerGroup(private val peerManager: PeerManager, val bloomFilterManager: BloomFilterManager, val network: NetworkParameters, private val peerSize: Int = 3) : Thread(), Peer.Listener {

    var blockSyncer: BlockSyncer? = null

    private val logger = Logger.getLogger("PeerGroup")
    private val peerMap = ConcurrentHashMap<String, Peer>()

    //    @Volatile???
    private var syncPeer: Peer? = null

    @Volatile
    private var running = false

    override fun run() {
        running = true
        // loop:
        while (running) {
            if (peerMap.size < peerSize) {
                startConnection()
            }

            try {
                Thread.sleep(2000L)
            } catch (e: InterruptedException) {
                break
            }
        }

        logger.info("Closing all peer connections...")
        for (conn in peerMap.values) {
            conn.close()
        }
    }

    private fun startConnection() {
        logger.info("Try open new peer connection...")
        val ip = peerManager.getPeerIp()
        if (ip != null) {
            logger.info("Try open new peer connection to $ip...")
            val peer = Peer(ip, network, this)
            peer.start()
        } else {
            logger.info("No peers found yet.")
        }
    }

    fun close() {
        running = false
        interrupt()
        try {
            join(5000)
        } catch (e: InterruptedException) {
        }
    }

    private val syncPeerQueue = Executors.newSingleThreadExecutor()

    override fun connected(peer: Peer) {
        peerMap[peer.host] = peer
        bloomFilterManager.bloomFilter?.let {
            peer.filterLoad(it)
        }

        assignNextSyncPeer()
    }

    private fun assignNextSyncPeer() {
        if (syncPeer != null) return

        syncPeerQueue.execute {
            peerMap.values.firstOrNull { it.connected && !it.synced }?.let { nonSyncedPeer ->
                syncPeer = nonSyncedPeer
                downloadBlockchain()
            }
        }
    }

    private fun downloadBlockchain() {
        bloomFilterManager.getUpdatedBloomFilter()?.let { bloomFilter ->
            peerMap.values.forEach { peer ->
                peer.filterLoad(bloomFilter)
            }
        }

        blockSyncer?.getBlockHashes()?.let { blockHashes ->
            if (blockHashes.isEmpty()) {
                syncPeer?.synced = syncPeer?.blockHashesSynced ?: false
            } else {
                syncPeer?.addTask(GetMerkleBlocksTask(blockHashes))
            }
        }

        if (syncPeer?.blockHashesSynced != true) {
            blockSyncer?.getBlockLocatorHashes()?.let { blockLocatorHashes ->
                syncPeer?.addTask(GetBlockHashesTask(blockLocatorHashes))
            }
        }

        if (syncPeer?.synced == true) {
            syncPeer = null
            assignNextSyncPeer()
        }
    }

    override fun disconnected(peer: Peer, e: Exception?) {
        if (e == null) {
            logger.info("PeerAddress $peer.host disconnected.")
            peerManager.markSuccess(peer.host)
        } else {
            logger.warning("PeerAddress $peer.host disconnected with error.")
            peerManager.markFailed(peer.host)
        }

        // it restores syncPeer on next connection
        if (syncPeer == peer) {
            blockSyncer?.clearBlockHashes()
            syncPeer = null
        }

        peerMap.remove(peer.host)
    }

    fun relay(transaction: Transaction) {
        TODO()
    }

    override fun onReady(peer: Peer) {
        if (peer == syncPeer) {
            downloadBlockchain()
        }
    }

    override fun onReceiveInventoryItems(peer: Peer, inventoryItems: List<InventoryItem>) {
        val blockHashes = inventoryItems.filter {
            it.type == InventoryItem.MSG_BLOCK && blockSyncer?.shouldRequest(it.hash) ?: false
        }.map { it.hash }

        if (blockHashes.isNotEmpty() && peer.synced) {
            peer.synced = false
            peer.blockHashesSynced = false
            assignNextSyncPeer()
        }

        val transactionHashes = inventoryItems.filter { it.type == InventoryItem.MSG_TX }.map { it.hash }

        transactionHashes.forEach {
            //        TODO("not implemented")
        }
    }

    override fun onTaskCompleted(peer: Peer, task: PeerTask) {
        when (task) {
            is GetBlockHashesTask -> {
                if (task.blockHashes.isEmpty()) {
                    peer.blockHashesSynced = true
                } else {
                    blockSyncer?.addBlockHashes(task.blockHashes)
                }
            }
            is GetMerkleBlocksTask -> {
                try {
                    blockSyncer?.handleMerkleBlocks(task.merkleBlocks)
                } catch (e: InvalidMerkleBlockException) {
                    peer.close()
                    TODO("wait for peer to disconnect?")
                    assignNextSyncPeer()
                }
            }
            else -> throw Exception("Task not handled: ${task}")
        }
    }
}
