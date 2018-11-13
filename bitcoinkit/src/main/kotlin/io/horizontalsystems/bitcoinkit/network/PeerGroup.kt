package io.horizontalsystems.bitcoinkit.network

import io.horizontalsystems.bitcoinkit.blocks.BlockSyncer
import io.horizontalsystems.bitcoinkit.crypto.BloomFilter
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.horizontalsystems.bitcoinkit.models.InventoryItem
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.models.NetworkAddress
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.network.PeerTask.*
import io.horizontalsystems.bitcoinkit.transactions.TransactionSyncer
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.logging.Logger

class PeerGroup(private val hostManager: PeerHostManager, private val bloomFilterManager: BloomFilterManager, private val network: NetworkParameters, private val peerSize: Int = 3) : Thread(), Peer.Listener, BloomFilterManager.Listener {

    interface LastBlockHeightListener {
        fun onReceiveMaxBlockHeight(height: Int)
    }

    var blockSyncer: BlockSyncer? = null
    var transactionSyncer: TransactionSyncer? = null
    var lastBlockHeightListener: LastBlockHeightListener? = null

    private val logger = Logger.getLogger("PeerGroup")
    private val peerMap = ConcurrentHashMap<String, Peer>()
    private var syncPeer: Peer? = null
    private var pendingTransactions: MutableList<Transaction> = mutableListOf()

    @Volatile
    private var running = false
    private val syncPeerQueue = Executors.newSingleThreadExecutor()
    private val localQueue = Executors.newSingleThreadExecutor()

    init {
        bloomFilterManager.listener = this
    }

    fun relay(transaction: Transaction) {
        localQueue.execute {
            pendingTransactions.add(transaction)
            dispatchTasks()
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

    //
    // Thread implementations
    //
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

    //
    // PeerListener implementations
    //
    override fun onConnect(peer: Peer) {
        peerMap[peer.host] = peer
        bloomFilterManager.bloomFilter?.let {
            peer.filterLoad(it)
        }

        assignNextSyncPeer()
    }

    override fun onReady(peer: Peer) {
        localQueue.execute {
            dispatchTasks(peer)
        }
    }

    override fun onDisconnect(peer: Peer, e: Exception?) {
        peerMap.remove(peer.host)

        if (e == null) {
            logger.info("Peer ${peer.host} disconnected.")
            hostManager.markSuccess(peer.host)
        } else {
            logger.warning("Peer ${peer.host} disconnected with error ${e.message}.")
            hostManager.markFailed(peer.host)
        }

        // it restores syncPeer on next connection
        if (syncPeer == peer) {
            blockSyncer?.downloadFailed()
            syncPeer = null
            assignNextSyncPeer()
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

    override fun onReceiveMerkleBlock(peer: Peer, merkleBlock: MerkleBlock) {
        try {
            blockSyncer?.handleMerkleBlock(merkleBlock)
        } catch (e: Exception) {
            peer.close(e)
        }
    }

    override fun onReceiveAddress(addrs: Array<NetworkAddress>) {
        val peerIps = mutableListOf<String>()
        for (address in addrs) {
            val addr = InetAddress.getByAddress(address.address)
            peerIps.add(addr.hostAddress)
        }

        hostManager.addPeers(peerIps.toTypedArray())
    }

    override fun onTaskComplete(peer: Peer, task: PeerTask) {
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

    //
    // BloomFilterManager implementations
    //
    override fun onFilterUpdated(bloomFilter: BloomFilter) {
        peerMap.values.forEach { peer ->
            peer.filterLoad(bloomFilter)
        }
    }

    //
    // Private methods
    //
    private fun startConnection() {
        logger.info("Try open new peer connection...")
        val ip = hostManager.getPeerIp()
        if (ip != null) {
            logger.info("Try open new peer connection to $ip...")
            val peer = Peer(ip, network, this)
            peer.localBestBlockHeight = blockSyncer?.localBestBlockHeight ?: 0
            peer.start()
        } else {
            logger.info("No peers found yet.")
        }
    }

    private fun assignNextSyncPeer() {
        syncPeerQueue.execute {
            if (syncPeer == null) {
                peerMap.values.firstOrNull { it.connected && !it.synced }?.let { nonSyncedPeer ->
                    syncPeer = nonSyncedPeer
                    blockSyncer?.downloadStarted()
                    logger.info("Start syncing peer ${nonSyncedPeer.host}")

                    lastBlockHeightListener?.onReceiveMaxBlockHeight(nonSyncedPeer.announcedLastBlockHeight)

                    downloadBlockchain()
                }
            }
        }
    }

    private fun downloadBlockchain() {
        syncPeer?.let { syncPeer ->
            blockSyncer?.let { blockSyncer ->

                val blockHashes = blockSyncer.getBlockHashes()
                if (blockHashes.isEmpty()) {
                    syncPeer.synced = syncPeer.blockHashesSynced
                } else {
                    syncPeer.addTask(GetMerkleBlocksTask(blockHashes))
                }

                if (!syncPeer.blockHashesSynced) {
                    syncPeer.addTask(GetBlockHashesTask(blockSyncer.getBlockLocatorHashes(syncPeer.announcedLastBlockHeight)))
                }

                if (syncPeer.synced) {
                    blockSyncer.downloadCompleted()
                    syncPeer.sendMempoolMessage()
                    logger.info("Peer synced ${syncPeer.host}")
                    this.syncPeer = null
                    assignNextSyncPeer()
                }
            }
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
