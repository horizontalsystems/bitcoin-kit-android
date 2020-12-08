package io.horizontalsystems.tools

import io.horizontalsystems.bitcoincore.core.IPeerAddressManager
import io.horizontalsystems.bitcoincore.core.IPeerAddressManagerListener
import io.horizontalsystems.bitcoincore.models.PeerAddress
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.network.peer.Peer
import io.horizontalsystems.bitcoincore.network.peer.PeerDiscover
import java.util.concurrent.CopyOnWriteArrayList

class PeerAddressManager(private val network: Network) : IPeerAddressManager {

    private val state = State()
    private val peerDiscover = PeerDiscover(this)

    override var listener: IPeerAddressManagerListener? = null

    override val hasFreshIps: Boolean
        get() {
            getLeastScoreFastestPeer()?.let { peerAddress ->
                return peerAddress.connectionTime == null
            }

            return false
        }

    override fun getIp(): String? {
        val peerAddress = getLeastScoreFastestPeer()
        if (peerAddress == null) {
            peerDiscover.lookup(network.dnsSeeds)
            return null
        }

        state.add(peerAddress.ip)

        return peerAddress.ip
    }

    override fun addIps(ips: List<String>) {
        state.setPeerAddresses(ips.map { PeerAddress(it, 0) })
        listener?.onAddAddress()
    }

    override fun markFailed(ip: String) {
        state.remove(ip)
        state.deletePeerAddress(ip)
    }

    override fun markSuccess(ip: String) {
        state.remove(ip)
        state.increasePeerAddressScore(ip)
    }

    override fun markConnected(peer: Peer) {
        state.setPeerConnectionTime(peer.host, peer.connectionTime)
    }

    private fun getLeastScoreFastestPeer(): PeerAddress? {
        return state.getLeastScoreFastestPeerAddressExcludingIps(state.getUsedPeers())
    }

    private class State {
        private val allPeers = mutableListOf<PeerAddress>()
        private var usedPeers = mutableListOf<String>()

        @Synchronized
        fun getUsedPeers(): List<String> {
            return usedPeers.toList()
        }

        @Synchronized
        fun add(ip: String) {
            usedPeers.add(ip)
        }

        @Synchronized
        fun remove(ip: String) {
            usedPeers.removeAll { it == ip }
        }

        @Synchronized
        fun getLeastScoreFastestPeerAddressExcludingIps(ips: List<String>): PeerAddress? {
            return allPeers.filter { !ips.contains(it.ip) }.sortedBy { it.connectionTime }.minBy { it.score }
        }

        @Synchronized
        fun increasePeerAddressScore(ip: String) {
            val peer = allPeers.find { it.ip == ip }
            if (peer != null) {
                peer.score += 1
            }
        }

        @Synchronized
        fun deletePeerAddress(ip: String) {
            allPeers.removeAll { it.ip == ip }
        }

        @Synchronized
        fun setPeerAddresses(list: List<PeerAddress>) {
            allPeers.addAll(list)
        }

        @Synchronized
        fun setPeerConnectionTime(ip: String, time: Long) {
            val peer = allPeers.find { it.ip == ip }
            if (peer != null) {
                peer.connectionTime = time
            }
        }
    }
}
