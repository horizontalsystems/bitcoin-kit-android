package io.horizontalsystems.bitcoincore.network.peer

import io.horizontalsystems.bitcoincore.crypto.BloomFilter
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.network.messages.*
import io.horizontalsystems.bitcoincore.network.peer.task.PeerTask
import java.net.InetAddress
import java.util.concurrent.ExecutorService

class Peer(
        val host: String,
        private val network: Network,
        private val listener: Listener,
        networkMessageParser: NetworkMessageParser,
        networkMessageSerializer: NetworkMessageSerializer,
        executorService: ExecutorService)
    : PeerConnection.Listener, PeerTask.Listener, PeerTask.Requester {

    interface Listener {
        fun onConnect(peer: Peer)
        fun onReady(peer: Peer)
        fun onDisconnect(peer: Peer, e: Exception?)
        fun onReceiveMessage(peer: Peer, message: IMessage)
        fun onTaskComplete(peer: Peer, task: PeerTask)
    }

    var blockHashesSynced = false
    var announcedLastBlockHeight = 0
    var localBestBlockHeight = 0
    var synced = false
    var connected = false
    var connectionTime: Long = 1000
    var tasks = mutableListOf<PeerTask>()

    private var connectStartTime: Long? = null
    private val peerConnection = PeerConnection(host, network, this, executorService, networkMessageParser, networkMessageSerializer)
    private val timer = PeerTimer()

    val ready: Boolean
        get() = connected && tasks.isEmpty()

    fun start(peerThreadPool: ExecutorService) {
        peerThreadPool.execute(peerConnection)
        connectStartTime = System.currentTimeMillis()
    }

    fun close(disconnectError: Exception? = null) {
        peerConnection.close(disconnectError)
    }

    fun addTask(task: PeerTask) {
        tasks.add(task)

        task.listener = this
        task.requester = this

        task.start()
    }

    fun filterLoad(bloomFilter: BloomFilter) {
        peerConnection.sendMessage(FilterLoadMessage(bloomFilter))
    }

    fun sendMempoolMessage() {
        peerConnection.sendMessage(MempoolMessage())
    }

    override fun equals(other: Any?): Boolean {
        if (other is Peer) {
            return host == other.host
        }

        return false
    }

    override fun hashCode(): Int {
        return host.hashCode()
    }

    //
    // PeerConnection Listener implementations
    //
    override fun onTimePeriodPassed() {
        try {
            timer.check()

            tasks.firstOrNull()?.checkTimeout()
        } catch (e: PeerTimer.Error.Idle) {
            val nonce = (Math.random() * Long.MAX_VALUE)
            send(PingMessage(nonce.toLong()))
            timer.pingSent()
        } catch (e: PeerTimer.Error.Timeout) {
            peerConnection.close(e)
        }
    }

    override fun onMessage(message: IMessage) {
        timer.restart()

        if (message is VersionMessage)
            return handleVersionMessage(message)
        if (message is VerAckMessage)
            return handleVerackMessage()

        if (!connected) return

        when (message) {
            is PingMessage -> peerConnection.sendMessage(PongMessage(message.nonce))
            is PongMessage -> {
            }
            is AddrMessage -> listener.onReceiveMessage(this, message)
            else -> if (tasks.none { it.handleMessage(message) }) {
                listener.onReceiveMessage(this, message)
            }
        }
    }

    override fun socketConnected(address: InetAddress) {
        peerConnection.sendMessage(VersionMessage(localBestBlockHeight, address, network))
        timer.restart()
    }

    override fun disconnected(e: Exception?) {
        connected = false
        listener.onDisconnect(this, e)
    }

    private fun handleVersionMessage(message: VersionMessage) = try {
        validatePeerVersion(message)

        announcedLastBlockHeight = message.lastBlock

        peerConnection.sendMessage(VerAckMessage())
    } catch (e: Error.UnsuitablePeerVersion) {
        close(e)
    }

    private fun handleVerackMessage() {
        if (connectStartTime == null) {
            return close()
        }

        connected = true
        connectStartTime?.let {
            connectionTime = System.currentTimeMillis() - it
        }
        listener.onConnect(this)
    }

    private fun validatePeerVersion(message: VersionMessage) {
        when {
            message.lastBlock <= 0 -> throw Error.UnsuitablePeerVersion("Peer last block is not greater than 0.")
            message.lastBlock < localBestBlockHeight -> throw Error.UnsuitablePeerVersion("Peer has expired blockchain ${message.lastBlock} vs ${localBestBlockHeight}(local)")
            !message.hasBlockChain(network) -> throw Error.UnsuitablePeerVersion("Peer does not have a copy of the block chain.")
            !message.supportsBloomFilter(network) -> throw Error.UnsuitablePeerVersion("Peer does not support Bloom Filter.")
            message.protocolVersion < network.protocolVersion -> throw Error.UnsuitablePeerVersion("Peer protocol version ${message.protocolVersion} vs ${network.protocolVersion}(local)")
        }
    }

    //
    // PeerTask Listener implementations
    //
    override fun onTaskCompleted(task: PeerTask) {
        tasks.find { it == task }?.let { completedTask ->
            tasks.remove(completedTask)
            listener.onTaskComplete(this, task)
        }

        // Reset timer for the next task in list
        tasks.firstOrNull()?.resetTimer()

        if (tasks.isEmpty()) {
            listener.onReady(this)
        }
    }

    override fun onTaskFailed(task: PeerTask, e: Exception) {
        peerConnection.close(e)
    }

    //
    // PeerTask Requester implementations
    //

    override val protocolVersion = network.protocolVersion

    override fun send(message: IMessage) {
        peerConnection.sendMessage(message)
    }

    open class Error(message: String) : Exception(message) {
        class UnsuitablePeerVersion(message: String) : Error(message)
    }
}
