package io.horizontalsystems.bitcoinkit.network.peer

class PeerManager {

    @Volatile
    var syncPeer: Peer? = null

    @Volatile
    private var peers = mutableListOf<Peer>()

    fun add(peer: Peer) {
        peers.add(peer)
    }

    fun remove(peer: Peer) {
        peers.removeAll { it == peer }
    }

    fun disconnectAll() {
        peers.forEach { it.close() }
        peers.clear()
    }

    fun peersCount(): Int {
        return peers.size
    }

    fun someReadyPeers(): List<Peer> {
        val readyPeers = peers.filter { it.ready }
        if (readyPeers.isEmpty()) {
            return listOf()
        }

        if (readyPeers.size == 1) {
            return readyPeers
        }

        return readyPeers.take(readyPeers.size / 2)
    }

    fun connected(): List<Peer> {
        return peers.filter { it.connected }
    }

    fun nonSyncedPeer(): Peer? {
        return peers.find { it.connected && !it.synced }
    }

    fun isSyncPeer(peer: Peer): Boolean {
        return peer == syncPeer
    }
}
