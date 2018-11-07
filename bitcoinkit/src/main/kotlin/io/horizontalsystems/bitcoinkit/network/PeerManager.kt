package io.horizontalsystems.bitcoinkit.network

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.models.PeerAddress
import io.realm.Realm
import java.util.logging.Logger

class PeerManager(private val network: NetworkParameters, private val realmFactory: RealmFactory) {

    private val logger = Logger.getLogger("PeerManager")
    private val usingPeers = mutableListOf<String>()
    private var peerDiscover = PeerDiscover()

    constructor(network: NetworkParameters, realmFactory: RealmFactory, peerDiscover: PeerDiscover) : this(network, realmFactory) {
        this.peerDiscover = peerDiscover
    }

    /**
     * Return a peer ip to connect.
     *
     * @return Ip or null if no peer available.
     */
    fun getPeerIp(): String? {
        logger.info("Try get an unused peer from peer addresses...")

        realmFactory.realm.use { realm ->
            val peerAddress = getUnusedPeer(realm)
            if (peerAddress == null) {
                try {
                    addPeers(peerDiscover.lookup(network.dnsSeeds))
                } catch (e: Exception) {
                    logger.warning("Could not discover peer addresses. ${e.message}")
                }

                return null
            }

            // mark peer as "using"
            usingPeers.add(peerAddress.ip)

            return peerAddress.ip
        }
    }

    fun markFailed(peerIp: String) {
        val realm = realmFactory.realm
        val peer = getUnusedPeer(realm, peerIp)
        if (peer != null) {
            usingPeers.removeAll { it == peerIp }
            realm.executeTransaction {
                peer.deleteFromRealm()
            }
        }
    }

    fun markSuccess(peerIp: String) {
        val realm = realmFactory.realm
        val peer = getUnusedPeer(realm, peerIp)
        if (peer != null) {
            realm.executeTransaction {
                peer.score += 3
            }
        }
    }

    private fun getUnusedPeer(realm: Realm, peerIp: String? = null): PeerAddress? {
        val query = realm.where(PeerAddress::class.java)
        if (peerIp != null) {
            query.equalTo("ip", peerIp)
        } else {
            query.not().`in`("ip", usingPeers.toTypedArray()).sort("score")
        }

        return query.findFirst()
    }

    private fun addPeers(ips: Array<String>) {
        logger.info("Add discovered " + ips.size + " peer addresses...")

        realmFactory.realm.use { realm ->
            realm.executeTransaction {
                ips.forEach { realm.insertOrUpdate(PeerAddress(it)) }
            }
        }

        logger.info("Total peer addresses: " + ips.size)
    }
}
