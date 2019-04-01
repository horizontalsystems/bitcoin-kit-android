package io.horizontalsystems.bitcoinkit.network.peer

import io.horizontalsystems.bitcoinkit.blocks.BlockSyncer
import io.horizontalsystems.bitcoinkit.core.ISyncStateListener
import io.horizontalsystems.bitcoinkit.crypto.BloomFilter
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.horizontalsystems.bitcoinkit.managers.ConnectionManager
import io.horizontalsystems.bitcoinkit.models.InventoryItem
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.models.NetworkAddress
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.bitcoinkit.network.peer.task.*
import io.horizontalsystems.bitcoinkit.transactions.TransactionSyncer
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.logging.Logger

class PeerGroup(
        private val hostManager: PeerHostManager,
        private val bloomFilterManager: BloomFilterManager,
        private val network: Network,
        private val syncStateListener: ISyncStateListener,
        private val peerSize: Int) : Thread(), Peer.Listener, BloomFilterManager.Listener {

    var blockSyncer: BlockSyncer? = null
    var transactionSyncer: TransactionSyncer? = null
    var connectionManager: ConnectionManager? = null

    @Volatile
    private var running = false
    private val logger = Logger.getLogger("PeerGroup")
    private val peersQueue = Executors.newSingleThreadExecutor()
    private val peerManager = PeerManager()

    init {
        bloomFilterManager.listener = this
    }

    @Throws
    fun sendPendingTransactions() {
        handlePendingTransactions()
    }

    @Throws
    fun checkPeersSynced() {
        if (peerManager.peersCount() < 1) {
            throw Error("No peers connected")
        }

        if (!peerManager.isHalfSynced()) {
            throw Error("Peers not synced yet")
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
        running = true

        syncStateListener.onSyncStart()
        blockSyncer?.prepareForDownload()

        while (running) {
            if (connectionManager?.isOnline == true && peerManager.peersCount() < peerSize) {
                startConnection()
            }

            try {
                Thread.sleep(2000L)
            } catch (e: InterruptedException) {
                break
            }
        }

        syncStateListener.onSyncStop()
        blockSyncer = null
        logger.info("Closing all peer connections...")

        peerManager.disconnectAll()
    }

    //
    // PeerListener implementations
    //
    override fun onConnect(peer: Peer) {
        peerManager.add(peer)

        bloomFilterManager.bloomFilter?.let {
            peer.filterLoad(it)
        }

        assignNextSyncPeer()
    }

    override fun onReady(peer: Peer) {
        peersQueue.execute {
            downloadBlockchain()
        }
    }

    override fun onDisconnect(peer: Peer, e: Exception?) {
        peerManager.remove(peer)

        if (e == null) {
            logger.info("Peer ${peer.host} disconnected.")
            hostManager.markSuccess(peer.host)
        } else {
            logger.warning("Peer ${peer.host} disconnected with error ${e.message}.")
            hostManager.markFailed(peer.host)
        }

        if (peerManager.isSyncPeer(peer)) {
            peerManager.syncPeer = null
            blockSyncer?.downloadFailed()
            assignNextSyncPeer()
        }
    }

    override fun onReceiveInventoryItems(peer: Peer, inventoryItems: List<InventoryItem>) {
        val blockHashes = mutableListOf<ByteArray>()
        val transactionHashes = mutableListOf<ByteArray>()

        inventoryItems.forEach { item ->
            when (item.type) {
                InventoryItem.MSG_BLOCK -> if (blockSyncer?.shouldRequest(item.hash) == true) {
                    blockHashes.add(item.hash)
                }
                InventoryItem.MSG_TX -> {
                    if (!isRequestingInventory(item.hash) && transactionSyncer?.shouldRequestTransaction(item.hash) == true) {
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
            blockSyncer?.handleMerkleBlock(merkleBlock, peer.announcedLastBlockHeight)
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
            is SendTransactionTask -> {
                transactionSyncer?.handleTransaction(task.transaction)
            }
            else -> throw Exception("Task not handled: $task")
        }
    }

    //
    // BloomFilterManager implementations
    //
    override fun onFilterUpdated(bloomFilter: BloomFilter) {
        peerManager.connected().forEach { peer ->
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
            peer.localBestBlockHeight = blockSyncer?.localDownloadedBestBlockHeight ?: 0
            peer.start()
        } else {
            logger.info("No peers found yet.")
        }
    }

    private fun assignNextSyncPeer() {
        peersQueue.execute {
            if (peerManager.syncPeer == null) {
                val nonSyncedPeer = peerManager.nonSyncedPeer()
                if (nonSyncedPeer == null) {
                    try {
                        handlePendingTransactions()
                    } catch (e: PeerGroup.Error) {
                        logger.warning("Handling pending transactions failed with: ${e.message}")
                    }
                } else {
                    peerManager.syncPeer = nonSyncedPeer
                    blockSyncer?.downloadStarted()

                    logger.info("Start syncing peer ${nonSyncedPeer.host}")

                    downloadBlockchain()
                }
            }
        }
    }

    private fun downloadBlockchain() {
        peerManager.syncPeer?.let { syncPeer ->
            blockSyncer?.let { blockSyncer ->

                val blockHashes = blockSyncer.getBlockHashes()
                if (blockHashes.isEmpty()) {
                    syncPeer.synced = syncPeer.blockHashesSynced
                } else {
                    syncPeer.addTask(GetMerkleBlocksTask(blockHashes))
                }

                if (!syncPeer.blockHashesSynced) {
                    val expectedHashesMinCount = Math.max(syncPeer.announcedLastBlockHeight - blockSyncer.localKnownBestBlockHeight, 0)
                    syncPeer.addTask(GetBlockHashesTask(blockSyncer.getBlockLocatorHashes(syncPeer.announcedLastBlockHeight), expectedHashesMinCount))
                }

                if (syncPeer.synced) {
                    blockSyncer.downloadCompleted()
                    syncStateListener.onSyncFinish()
                    syncPeer.sendMempoolMessage()
                    logger.info("Peer synced ${syncPeer.host}")
                    peerManager.syncPeer = null
                    assignNextSyncPeer()
                }
            }
        }
    }

    @Throws
    private fun handlePendingTransactions() {
        checkPeersSynced()

        peerManager.someReadyPeers().forEach { peer ->
            transactionSyncer?.getPendingTransactions()?.forEach { pendingTransaction ->
                peer.addTask(SendTransactionTask(pendingTransaction))
            }
        }
    }

    private fun isRequestingInventory(hash: ByteArray): Boolean {
        return peerManager.connected().any { peer -> peer.isRequestingInventory(hash) }
    }

    //
    // PeerGroup Exceptions
    //
    class Error(message: String) : Exception(message)
}
