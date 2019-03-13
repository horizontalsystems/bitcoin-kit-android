package io.horizontalsystems.bitcoinkit.network.peer

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.models.PeerAddress
import io.horizontalsystems.bitcoinkit.network.Network
import java.util.logging.Logger

class PeerAddressManager(private val network: Network, private val storage: IStorage) {

    private val state = State()
    private val logger = Logger.getLogger("PeerHostManager")
    private val peerDiscover = PeerDiscover(this)

    fun getIp(): String? {
        logger.info("Try get an unused peer from peer addresses...")

        val peerAddress = storage.getLeastScorePeerAddressExcludingIps(state.usedPeers)
        if (peerAddress == null) {
            peerDiscover.lookup(network.dnsSeeds)
            return null
        }

        state.add(peerAddress.ip)

        return peerAddress.ip
    }

    fun addIps(ips: Array<String>) {
        logger.info("Add discovered ${ips.size} peer addresses...")

        val newPeerIps = ips.distinct()
        val existingPeers = storage.getExistingPeerAddress(newPeerIps).map { it.ip }

        val peerAddresses = newPeerIps.subtract(existingPeers).map {
            PeerAddress(it, 0)
        }

        storage.setPeerAddresses(peerAddresses)

        logger.info("Total peer addresses: ${ips.size}")
    }

    fun markFailed(ip: String) {
        state.remove(ip)

        storage.deletePeerAddress(ip)
    }

    fun markSuccess(ip: String) {
        state.remove(ip)

        storage.increasePeerAddressScore(ip)
    }

    class State {
        var usedPeers = mutableListOf<String>()
            private set

        fun add(ip: String) {
            synchronized(usedPeers) {
                usedPeers.add(ip)
            }
        }

        fun remove(ip: String) {
            synchronized(usedPeers) {
                usedPeers.removeAll { it == ip }
            }
        }
    }
}
