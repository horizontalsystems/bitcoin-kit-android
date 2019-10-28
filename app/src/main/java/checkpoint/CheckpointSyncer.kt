package checkpoint

import android.util.Log
import io.horizontalsystems.bitcoincore.core.DoubleSha256Hasher
import io.horizontalsystems.bitcoincore.core.IConnectionManager
import io.horizontalsystems.bitcoincore.core.IConnectionManagerListener
import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.network.messages.*
import io.horizontalsystems.bitcoincore.network.peer.IPeerTaskHandler
import io.horizontalsystems.bitcoincore.network.peer.Peer
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup
import io.horizontalsystems.bitcoincore.network.peer.PeerManager
import io.horizontalsystems.bitcoincore.network.peer.task.GetBlockHeadersTask
import io.horizontalsystems.bitcoincore.network.peer.task.PeerTask
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

class CheckpointSyncer(
        private val network: Network,
        private val peerSize: Int)
    : PeerGroup.Listener, IPeerTaskHandler {

    private val syncedPeers = CopyOnWriteArrayList<Peer>()
    private val peerManager = PeerManager()
    private val checkpointBlock = network.lastCheckpointBlock

    @Volatile
    private var syncPeer: Peer? = null
    private val peersQueue = Executors.newSingleThreadExecutor()

    private val checkpoints = mutableListOf(BlockHash(checkpointBlock.headerHash, checkpointBlock.height))
    private val blockHashes = LinkedList<BlockHash>().also {
        it.add(checkpoints.last())
    }

    fun start() {
        val localDownloadedBestBlockHeight = 0

        val networkMessageParser = NetworkMessageParser(network.magic).apply {
            add(VersionMessageParser())
            add(VerAckMessageParser())
            add(InvMessageParser())
            add(HeadersMessageParser(DoubleSha256Hasher()))
        }

        val networkMessageSerializer = NetworkMessageSerializer(network.magic).apply {
            add(VersionMessageSerializer())
            add(VerAckMessageSerializer())
            add(InvMessageSerializer())
            add(GetHeadersMessageSerializer())
            add(HeadersMessageSerializer())
        }

        val connectionManager = object : IConnectionManager {
            override val listener: IConnectionManagerListener? = null
            override val isConnected = true
        }

        val peerHostManager = PeerAddressManager(network)
        val peerGroup = PeerGroup(peerHostManager, network, peerManager, peerSize, networkMessageParser, networkMessageSerializer, connectionManager, localDownloadedBestBlockHeight).also {
            peerHostManager.listener = it
        }

        peerGroup.addPeerGroupListener(this)
        peerGroup.peerTaskHandler = this
        peerGroup.start()
    }

    //  PeerGroup Listener

    override fun onPeerConnect(peer: Peer) {
        Log.e("AAA", "onPeerConnect ${peer.host}")
        assignNextSyncPeer()
    }

    override fun onPeerDisconnect(peer: Peer, e: Exception?) {
        syncedPeers.remove(peer)

        if (peer == syncPeer) {
            syncPeer = null
            assignNextSyncPeer()
        }
    }

    override fun onPeerReady(peer: Peer) {
        Log.e("AAA", "onPeerReady ${peer.host}")
    }

    //  IPeerTaskHandler

    override fun handleCompletedTask(peer: Peer, task: PeerTask): Boolean {
        return when (task) {
            is GetBlockHeadersTask -> {
                validateHeaders(peer, task.blockHeaders)
                true
            }
            else -> false
        }
    }

    private fun validateHeaders(peer: Peer, headers: Array<BlockHeader>) {
        var block = blockHashes.last()
        for (header in headers) {
            if (!block.headerHash.contentEquals(header.previousBlockHeaderHash)) {
                syncPeer = null
                assignNextSyncPeer()
                break
            }

            val blockHash = BlockHash(header.hash, block.height + 1)
            if (blockHash.height % 2016 == 0) {
                checkpoints.add(blockHash)
            }

            blockHashes.add(blockHash)
            block = blockHash
        }

        if (headers.size < 2000) {
            peer.synced = true
            peer.blockHashesSynced = true
            return
        }

        downloadBlockchain()
    }

    private fun assignNextSyncPeer() {
        peersQueue.execute {
            if (syncPeer == null) {
                val notSyncedPeers = peerManager.sorted().filter { !it.synced }
                notSyncedPeers.firstOrNull { it.ready }?.let { nonSyncedPeer ->
                    syncPeer = nonSyncedPeer

                    Log.e("AAA", "Start syncing peer ${nonSyncedPeer.host}")

                    downloadBlockchain()
                }
            }
        }
    }

    private fun downloadBlockchain() {
        val peer = syncPeer
        if (peer == null || !peer.ready) {
            return
        }

        peer.addTask(GetBlockHeadersTask(getBlockLocatorHashes()))
    }

    //  PeerTaskHandler

    private fun getBlockLocatorHashes(): List<ByteArray> {
        return if (blockHashes.isEmpty()) {
            listOf(checkpoints.last().headerHash)
        } else {
            listOf(blockHashes.last().headerHash)
        }
    }
}
