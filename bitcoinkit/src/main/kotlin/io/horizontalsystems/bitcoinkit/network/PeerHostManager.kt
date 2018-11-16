package io.horizontalsystems.bitcoinkit.network

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.models.PeerAddress
import io.realm.Realm
import java.util.logging.Logger

class PeerHostManager(private val network: NetworkParameters, private val realmFactory: RealmFactory) {

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
        realmFactory.realm.use { realm ->
            getPeer(realm, peerIp)?.let { peer ->
                usingPeers.removeAll { it == peerIp }
                realm.executeTransaction {
                    peer.deleteFromRealm()
                }
            }
        }
    }

    fun markSuccess(peerIp: String) {
        realmFactory.realm.use { realm ->
            getPeer(realm, peerIp)?.let { peer ->
                usingPeers.removeAll { it == peerIp }
                realm.executeTransaction {
                    peer.score += 3
                }
            }
        }
    }

    fun addPeers(ips: Array<String>) {
        logger.info("Add discovered " + ips.size + " peer addresses...")

        realmFactory.realm.use { realm ->
            realm.executeTransaction {
                ips.forEach { ip -> realm.insertOrUpdate(PeerAddress(ip)) }
            }
        }

        logger.info("Total peer addresses: " + ips.size)
    }

    private fun getUnusedPeer(realm: Realm): PeerAddress? {
        return realm.where(PeerAddress::class.java).not()
                .`in`("ip", usingPeers.toTypedArray())
                .sort("score")
                .findFirst()
    }

    private fun getPeer(realm: Realm, peerIp: String): PeerAddress? {
        return realm.where(PeerAddress::class.java)
                .equalTo("ip", peerIp)
                .findFirst()
    }
}
