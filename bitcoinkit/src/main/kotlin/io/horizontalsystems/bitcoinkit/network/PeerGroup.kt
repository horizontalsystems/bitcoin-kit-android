package io.horizontalsystems.bitcoinkit.network

import io.horizontalsystems.bitcoinkit.blocks.BlockSyncer
import io.horizontalsystems.bitcoinkit.crypto.BloomFilter
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.horizontalsystems.bitcoinkit.models.InventoryItem
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.network.PeerTask.GetBlockHashesTask
import io.horizontalsystems.bitcoinkit.network.PeerTask.GetMerkleBlocksTask
import io.horizontalsystems.bitcoinkit.network.PeerTask.PeerTask
import io.horizontalsystems.bitcoinkit.network.PeerTask.RequestTransactionsTask
import io.horizontalsystems.bitcoinkit.transactions.TransactionSyncer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.logging.Logger

class PeerGroup(private val peerManager: PeerManager, val bloomFilterManager: BloomFilterManager, val network: NetworkParameters, private val peerSize: Int = 3) : Thread(), Peer.Listener, BloomFilterManager.Listener {

    interface LastBlockHeightListener {
        fun onReceiveBestBlockHeight(lastBlockHeight: Int)
    }

    var blockSyncer: BlockSyncer? = null
    var transactionSyncer: TransactionSyncer? = null
    var lastBlockHeightListener: LastBlockHeightListener? = null

    private val logger = Logger.getLogger("PeerGroup")
    private val peerMap = ConcurrentHashMap<String, Peer>()

    //    @Volatile???
    private var syncPeer: Peer? = null

    @Volatile
    private var running = false

    init {
        bloomFilterManager.listener = this
    }

    override fun run() {
        blockSyncer?.prepareForDownload()

        running = true
        addNonSentTransactions()
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
        syncPeerQueue.execute {
            if (syncPeer == null) {
                peerMap.values.firstOrNull { it.connected && !it.synced }?.let { nonSyncedPeer ->
                    syncPeer = nonSyncedPeer
                    blockSyncer?.downloadStarted()
                    downloadBlockchain()
                }
            }
        }
    }

    private fun downloadBlockchain() {
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
            blockSyncer?.downloadCompleted()
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
            blockSyncer?.downloadFailed()
            syncPeer = null
            assignNextSyncPeer()
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
        val blockHashes = mutableListOf<ByteArray>()
        val transactionHashes = mutableListOf<ByteArray>()

        inventoryItems.forEach { item ->
            when (item.type) {
                InventoryItem.MSG_BLOCK -> {
                    if (blockSyncer?.shouldRequest(item.hash) == true) {
                        blockHashes.add(item.hash)
                    }
                }
                InventoryItem.MSG_TX -> {
                    if (!isRequestingInventory(item.hash) && !handleRelayedTransaction(item.hash) && transactionSyncer?.shouldRequestTransaction(item.hash) == true) {
                        transactionHashes.add(item.hash)
                    }
                }
            }
        }

        if (blockHashes.isNotEmpty() && peer.synced) {
            peer.synced = false
            peer.blockHashesSynced = false
            assignNextSyncPeer()
        }

        if (transactionHashes.isNotEmpty()) {
            peer.addTask(RequestTransactionsTask(transactionHashes))
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
                blockSyncer?.downloadIterationCompleted()
            }
            is RequestTransactionsTask -> {
                transactionSyncer?.handleTransactions(task.transactions)
            }
            else -> throw Exception("Task not handled: ${task}")
        }
    }

    override fun handleMerkleBlock(peer: Peer, merkleBlock: MerkleBlock) {
        try {
            blockSyncer?.handleMerkleBlock(merkleBlock)
        } catch (e: Exception) {
            peer.close()
        }
    }

    override fun onReceiveBestBlockHeight(peer: Peer, lastBlockHeight: Int) {
        lastBlockHeightListener?.onReceiveBestBlockHeight(lastBlockHeight)
    }

    override fun onFilterUpdated(bloomFilter: BloomFilter) {
        peerMap.values.forEach { peer ->
            peer.filterLoad(bloomFilter)
        }
    }

    private fun handleRelayedTransaction(hash: ByteArray): Boolean {
        return peerMap.any { (_, peer) -> peer.handleRelayedTransaction(hash) }
    }

    private fun isRequestingInventory(hash: ByteArray): Boolean {
        return peerMap.any { (_, peer) -> peer.isRequestingInventory(hash) }
    }

    private fun addNonSentTransactions() {
        transactionSyncer?.getNonSentTransactions()?.let { transactions ->
            transactions.forEach { relay(it) }
        }
    }
}
