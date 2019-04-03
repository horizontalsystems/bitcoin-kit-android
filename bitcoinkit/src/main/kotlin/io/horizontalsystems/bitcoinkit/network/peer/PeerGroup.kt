package io.horizontalsystems.bitcoinkit.network.peer

import io.horizontalsystems.bitcoinkit.managers.ConnectionManager
import io.horizontalsystems.bitcoinkit.models.InventoryItem
import io.horizontalsystems.bitcoinkit.models.NetworkAddress
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.bitcoinkit.network.peer.task.PeerTask
import java.net.InetAddress
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.logging.Logger

class PeerGroup(
        private val hostManager: PeerAddressManager,
        private val network: Network,
        private val peerManager: PeerManager,
        private val peerSize: Int) : Thread(), Peer.Listener {

    interface IPeerGroupListener {
        fun onStart() = Unit
        fun onStop() = Unit
        fun onPeerCreate(peer: Peer) = Unit
        fun onPeerConnect(peer: Peer) = Unit
        fun onPeerDisconnect(peer: Peer, e: Exception?) = Unit
        fun onPeerReady(peer: Peer) = Unit
    }

    var connectionManager: ConnectionManager? = null

    var inventoryItemsHandler: IInventoryItemsHandler? = null
    var peerTaskHandler: IPeerTaskHandler? = null

    @Volatile
    private var running = false
    private val logger = Logger.getLogger("PeerGroup")
    private val peersQueue = Executors.newSingleThreadExecutor()
    private val taskQueue: BlockingQueue<PeerTask> = ArrayBlockingQueue(10)
    private val peerGroupListeners = mutableListOf<IPeerGroupListener>()

    fun addPeerGroupListener(listener: IPeerGroupListener) {
        peerGroupListeners.add(listener)
    }

    fun someReadyPeers(): List<Peer> {
        return peerManager.someReadyPeers()
    }

    @Throws
    fun checkPeersSynced() {
        if (peerManager.peersCount() < 1) {
            throw Error("No peers connected")
        }

        if (peerManager.nonSyncedPeer() != null) {
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

        peerGroupListeners.forEach { it.onStart() }

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

        peerGroupListeners.forEach { it.onStop() }
        logger.info("Closing all peer connections...")

        peerManager.disconnectAll()
    }

    //
    // PeerListener implementations
    //
    override fun onConnect(peer: Peer) {
        peerManager.add(peer)

        peerGroupListeners.forEach { it.onPeerConnect(peer) }
    }

    override fun onReady(peer: Peer) {
        peersQueue.execute {
            peerGroupListeners.forEach { it.onPeerReady(peer) }

//            todo check if peer is not syncPeer
            taskQueue.poll()?.let {
                peer.addTask(it)
            }
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

        peerGroupListeners.forEach { it.onPeerDisconnect(peer, e) }

    }

    override fun onReceiveInventoryItems(peer: Peer, inventoryItems: List<InventoryItem>) {
        inventoryItemsHandler?.handleInventoryItems(peer, inventoryItems)
    }

    override fun onReceiveAddress(addrs: Array<NetworkAddress>) {
        val peerIps = mutableListOf<String>()
        for (address in addrs) {
            val addr = InetAddress.getByAddress(address.address)
            peerIps.add(addr.hostAddress)
        }

        hostManager.addIps(peerIps.toTypedArray())
    }

    override fun onTaskComplete(peer: Peer, task: PeerTask) {
        peerTaskHandler?.handleCompletedTask(peer, task)
    }

    //
    // Private methods
    //
    private fun startConnection() {
        logger.info("Try open new peer connection...")
        val ip = hostManager.getIp()
        if (ip != null) {
            logger.info("Try open new peer connection to $ip...")
            val peer = Peer(ip, network, this)
            peerGroupListeners.forEach { it.onPeerCreate(peer) }
            peer.start()
        } else {
            logger.info("No peers found yet.")
        }
    }

    fun addTask(peerTask: PeerTask) {
        // todo find better solution
        val peer = peerManager.someReadyPeers().firstOrNull()

        if (peer == null) {
            taskQueue.add(peerTask)
        } else {
            peer.addTask(peerTask)
        }

    }

    //
    // PeerGroup Exceptions
    //
    class Error(message: String) : Exception(message)
}
