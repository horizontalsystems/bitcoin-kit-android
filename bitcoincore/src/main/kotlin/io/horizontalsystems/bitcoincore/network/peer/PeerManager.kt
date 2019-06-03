package io.horizontalsystems.bitcoincore.network.peer

import java.util.concurrent.ConcurrentHashMap

class PeerManager {

    private var peers = ConcurrentHashMap<String, Peer>()

    val peersCount: Int
        get() = peers.size

    fun add(peer: Peer) {
        peers[peer.host] = peer
    }

    fun remove(peer: Peer) {
        peers.remove(peer.host)
    }

    fun disconnectAll() {
        peers.values.forEach { it.close() }
        peers.clear()
    }

    fun someReadyPeers(): List<Peer> {
        val readyPeers = peers.values.filter { it.ready }
        if (readyPeers.isEmpty()) {
            return listOf()
        }

        if (readyPeers.size == 1) {
            return readyPeers
        }

        return readyPeers.take(readyPeers.size / 2)
    }

    fun connected(): List<Peer> {
        return peers.values.filter { it.connected }
    }

    fun isHalfSynced(): Boolean {
        return (peers.size / 2) <= peers.values.filter { it.connected && it.synced }.size
    }

}
