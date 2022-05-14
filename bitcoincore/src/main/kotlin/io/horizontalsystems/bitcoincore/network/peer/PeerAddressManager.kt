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
        var peerAddress = if (network.isSafe()) {
            getLeastScoreFastestPeerSafe()
        } else {
            getLeastScoreFastestPeer()
        }
        // Safe 优先从主节点中取节点同步
        if (!network.isMainNode(peerAddress?.ip)) {
            val ip = network.getMainNodeIp(state.getUsedPeers())
            if (ip != null) {
                return ip
            }
            peerAddress = getLeastScoreFastestPeer()
        }
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

    override fun saveLastBlock(ip: String, lastBlock: Int) {
        storage.saveLastBlock(ip, lastBlock)
    }

    private fun getLeastScoreFastestPeer(): PeerAddress? {
        return storage.getLeastScoreFastestPeerAddressExcludingIps(state.getUsedPeers())
    }

    private fun getLeastScoreFastestPeerSafe(): PeerAddress? {
        return storage.getLeastScoreFastestPeerAddressExcludingIpsSafe(state.getUsedPeers())
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
