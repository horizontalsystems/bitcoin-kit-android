package io.horizontalsystems.bitcoinkit.network

import io.horizontalsystems.bitcoinkit.blocks.BlockSyncer
import io.horizontalsystems.bitcoinkit.crypto.BloomFilter
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.horizontalsystems.bitcoinkit.models.InventoryItem
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.network.PeerTask.*
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
    private var pendingTransactions: MutableList<Transaction> = mutableListOf()

    //    @Volatile???
    private var syncPeer: Peer? = null

    @Volatile
    private var running = false
    private val syncPeerQueue = Executors.newSingleThreadExecutor()
    private val localQueue = Executors.newSingleThreadExecutor()

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
                    logger.info("Start syncing peer ${nonSyncedPeer.host}")
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
            syncPeer?.sendMempoolMessage()
            logger.info("Peer synced ${syncPeer?.host}")
            syncPeer = null
            assignNextSyncPeer()
        }
    }

    override fun disconnected(peer: Peer, e: Exception?) {
        if (e == null) {
            logger.info("Peer ${peer.host} disconnected.")
            peerManager.markSuccess(peer.host)
        } else {
            logger.warning("Peer ${peer.host} disconnected with error ${e.message}.")
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
        localQueue.execute {
            pendingTransactions.add(transaction)
            dispatchTasks()
        }
    }

    override fun onReady(peer: Peer) {
        localQueue.execute {
            dispatchTasks(peer)
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

    private fun dispatchTasks(peer: Peer? = null) {
        if (peer != null)
            return handleReady(peer)

        peerMap.values.filter { it.ready }.forEach {
            handleReady(it)
        }
    }

    private fun handleReady(peer: Peer) {
        if (!peer.ready)
            return

        if (peer == syncPeer)
            return downloadBlockchain()

        pendingTransactions.forEach { peer.addTask(RelayTransactionTask(it)) }
        pendingTransactions = mutableListOf()
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
