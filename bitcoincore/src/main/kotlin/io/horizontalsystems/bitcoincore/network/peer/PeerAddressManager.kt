package io.horizontalsystems.bitcoincore.network.peer

import io.horizontalsystems.bitcoincore.core.IPeerAddressManager
import io.horizontalsystems.bitcoincore.core.IPeerAddressManagerListener
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.PeerAddress
import io.horizontalsystems.bitcoincore.network.Network
import java.util.logging.Logger

class PeerAddressManager(private val network: Network, private val storage: IStorage) : IPeerAddressManager {

    override var listener: IPeerAddressManagerListener? = null

    private val state = State()
    private val logger = Logger.getLogger("PeerHostManager")
    private val peerDiscover = PeerDiscover(this)

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
        storage.setPeerAddresses(ips.map { PeerAddress(it, 0) })

        logger.info("Added new addresses: ${ips.size}")

        listener?.onAddAddress()
    }

    override fun markFailed(ip: String) {
        state.remove(ip)

        storage.deletePeerAddress(ip)
    }

    override fun markSuccess(ip: String) {
        state.remove(ip)
    }

    override fun markConnected(peer: Peer) {
        storage.markConnected(peer.host, peer.connectionTime)
    }

    private fun getLeastScoreFastestPeer(): PeerAddress? {
        return storage.getLeastScoreFastestPeerAddressExcludingIps(state.getUsedPeers())
    }

    private class State {
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
    }
}
