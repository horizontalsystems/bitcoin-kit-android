package io.horizontalsystems.bitcoincore.network.peer

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.PeerAddress
import io.horizontalsystems.bitcoincore.network.Network
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Logger

class PeerAddressManager(private val network: Network, private val storage: IStorage) {

    interface Listener {
        fun onAddAddress()
    }

    var listener: Listener? = null

    private val state = State()
    private val logger = Logger.getLogger("PeerHostManager")
    private val peerDiscover = PeerDiscover(this)

    val hasFreshIps: Boolean
        get() {
            getLeastScoreFastestPeer()?.let { peerAddress ->
                return peerAddress.connectionTime == null
            }

            return false
        }

    fun getIp(): String? {
        val peerAddress = getLeastScoreFastestPeer()
        if (peerAddress == null) {
            peerDiscover.lookup(network.dnsSeeds)
            return null
        }

        state.add(peerAddress.ip)

        return peerAddress.ip
    }

    fun addIps(ips: List<String>) {
        storage.setPeerAddresses(ips.map { PeerAddress(it, 0) })

        logger.info("Added new addresses: ${ips.size}")

        listener?.onAddAddress()
    }

    fun markFailed(ip: String) {
        state.remove(ip)

        storage.deletePeerAddress(ip)
    }

    fun markSuccess(ip: String) {
        state.remove(ip)

        storage.increasePeerAddressScore(ip)
    }

    fun markConnected(peer: Peer) {
        storage.setPeerConnectionTime(peer.host, peer.connectionTime)
    }

    private fun getLeastScoreFastestPeer(): PeerAddress? {
        return storage.getLeastScoreFastestPeerAddressExcludingIps(state.usedPeers.toList())
    }

    private class State {
        var usedPeers = CopyOnWriteArrayList<String>()
            private set

        fun add(ip: String) {
            usedPeers.add(ip)
        }

        fun remove(ip: String) {
            usedPeers.removeAll { it == ip }
        }
    }
}
