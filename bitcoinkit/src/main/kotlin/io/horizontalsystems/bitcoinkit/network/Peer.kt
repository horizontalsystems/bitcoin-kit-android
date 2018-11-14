package io.horizontalsystems.bitcoinkit.network

import io.horizontalsystems.bitcoinkit.blocks.MerkleBlockExtractor
import io.horizontalsystems.bitcoinkit.crypto.BloomFilter
import io.horizontalsystems.bitcoinkit.messages.*
import io.horizontalsystems.bitcoinkit.models.InventoryItem
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.models.NetworkAddress
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.network.PeerTask.IPeerTaskDelegate
import io.horizontalsystems.bitcoinkit.network.PeerTask.IPeerTaskRequester
import io.horizontalsystems.bitcoinkit.network.PeerTask.PeerTask
import java.net.InetAddress

class Peer(val host: String, private val network: NetworkParameters, private val listener: Listener) : PeerConnection.Listener, IPeerTaskDelegate, IPeerTaskRequester {

    interface Listener {
        fun onConnect(peer: Peer)
        fun onReady(peer: Peer)
        fun onDisconnect(peer: Peer, e: Exception?)
        fun onReceiveInventoryItems(peer: Peer, inventoryItems: List<InventoryItem>)
        fun onReceiveMerkleBlock(peer: Peer, merkleBlock: MerkleBlock)
        fun onReceiveAddress(addrs: Array<NetworkAddress>)
        fun onTaskComplete(peer: Peer, task: PeerTask)
    }

    var connected = false
    var synced = false
    var blockHashesSynced = false
    var announcedLastBlockHeight: Int = 0
    var localBestBlockHeight: Int = 0

    private val merkleBlockExtractor = MerkleBlockExtractor(network.maxBlockSize)
    private val peerConnection = PeerConnection(host, network, this)
    private var tasks = mutableListOf<PeerTask>()

    val ready: Boolean
        get() = connected && tasks.isEmpty()

    fun start() {
        peerConnection.start()
    }

    fun close(disconnectError: Exception? = null) {
        peerConnection.close(disconnectError)
    }

    fun addTask(task: PeerTask) {
        tasks.add(task)

        task.delegate = this
        task.requester = this

        task.start()
    }

    fun filterLoad(bloomFilter: BloomFilter) {
        peerConnection.sendMessage(FilterLoadMessage(bloomFilter))
    }

    fun sendMempoolMessage() {
        peerConnection.sendMessage(MempoolMessage())
    }

    fun isRequestingInventory(hash: ByteArray): Boolean {
        return tasks.any { it.isRequestingInventory(hash) }
    }

    //
    // PeerConnection Listener implementations
    //
    override fun onMessage(message: Message) {
        when (message) {
            is PingMessage -> peerConnection.sendMessage(PongMessage(message.nonce))
            is PongMessage -> handlePongMessage(message)
            is VersionMessage -> handleVersionMessage(message)
            is VerAckMessage -> handleVerackMessage()
            is AddrMessage -> handleAddrMessage(message)
            is MerkleBlockMessage -> handleMerkleBlockMessage(message)
            is TransactionMessage -> handleTransactionMessage(message)
            is InvMessage -> handleInvMessage(message)
            is GetDataMessage -> {
                for (inv in message.inventory) {
                    if (tasks.any { it.handleGetDataInventoryItem(inv) }) {
                        continue
                    }
                }
            }
        }
    }

    override fun socketConnected(address: InetAddress) {
        peerConnection.sendMessage(VersionMessage(localBestBlockHeight, address, network))
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
        connected = true

        listener.onConnect(this)
    }

    private fun handleAddrMessage(message: AddrMessage) {
        listener.onReceiveAddress(message.addresses)
    }

    private fun validatePeerVersion(message: VersionMessage) {
        when {
            message.lastBlock <= 0 -> throw Error.UnsuitablePeerVersion("Peer last block is not greater than 0.")
            message.lastBlock < localBestBlockHeight -> throw Error.UnsuitablePeerVersion("Peer has expired blockchain ${message.lastBlock} vs ${localBestBlockHeight}(local)")
            !message.hasBlockChain(network) -> throw Error.UnsuitablePeerVersion("Peer does not have a copy of the block chain.")
            !message.supportsBloomFilter(network) -> throw Error.UnsuitablePeerVersion("Peer does not support Bloom Filter.")
        }
    }

    //
    // IPeerTaskDelegate implementations
    //
    override fun onTaskCompleted(task: PeerTask) {
        tasks.firstOrNull { it == task }?.let { completedTask ->
            tasks.remove(completedTask)
            listener.onTaskComplete(this, task)
        }

        if (tasks.isEmpty()) {
            listener.onReady(this)
        }
    }

    override fun onTaskFailed(task: PeerTask, e: Exception) {
        peerConnection.close(e)
    }

    override fun handleMerkleBlock(merkleBlock: MerkleBlock) {
        listener.onReceiveMerkleBlock(this, merkleBlock)
    }

    //
    // IPeerTaskRequester implementations
    //
    override fun getBlocks(hashes: List<ByteArray>) {
        peerConnection.sendMessage(GetBlocksMessage(hashes, network))
    }

    override fun getData(items: List<InventoryItem>) {
        peerConnection.sendMessage(GetDataMessage(items))
    }

    override fun ping(nonce: Long) {
        peerConnection.sendMessage(PingMessage(nonce))
    }

    override fun sendTransactionInventory(hash: ByteArray) {
        peerConnection.sendMessage(InvMessage(InventoryItem.MSG_TX, hash))
    }

    override fun send(transaction: Transaction) {
        peerConnection.sendMessage(TransactionMessage(transaction))
    }

    //
    // Private methods
    //
    private fun handleInvMessage(message: InvMessage) {
        for (task in tasks) {
            if (task.handleInventoryItems(message.inventory)) {
                return
            }
        }

        listener.onReceiveInventoryItems(this, message.inventory)
    }

    private fun handleMerkleBlockMessage(message: MerkleBlockMessage) {
        for (task in tasks) {
            if (task.handleMerkleBlock(merkleBlockExtractor.extract(message))) {
                return
            }
        }
    }

    private fun handleTransactionMessage(message: TransactionMessage) {
        for (task in tasks) {
            if (task.handleTransaction(message.transaction)) {
                return
            }
        }
    }

    private fun handlePongMessage(message: PongMessage) {
        for (task in tasks) {
            if (task.handlePong(message.nonce)) {
                return
            }
        }
    }

    open class Error(message: String) : Exception(message) {
        class UnsuitablePeerVersion(message: String) : Error(message)
    }

}
