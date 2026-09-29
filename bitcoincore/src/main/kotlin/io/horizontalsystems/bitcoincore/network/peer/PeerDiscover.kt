package io.horizontalsystems.bitcoincore.network.peer

import io.horizontalsystems.bitcoincore.core.IPeerAddressManager
import io.horizontalsystems.bitcoincore.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.logging.Logger

class PeerDiscover(private val peerAddressManager: IPeerAddressManager) {

    private val logger = Logger.getLogger("PeerDiscover")

    private var lookupInProgress = false
    private var nextLookupTime = 0L

    fun lookup(dnsList: List<String>) {
        // Adding the looked-up addresses asks the peer group for peers again, which lands here
        // again when none of them is free, so without this the seeds are queried in a loop
        synchronized(this) {
            if (lookupInProgress || System.currentTimeMillis() < nextLookupTime) return
            lookupInProgress = true
        }

        logger.info("Lookup peers from DNS seed...")

        // todo: launch coroutines for each dns resolve
        // Resolving blocks on network I/O, up to the SOCKS timeout per seed
        GlobalScope.launch(Dispatchers.IO) {
            var found = false
            try {
                found = lookupSeeds(dnsList)
            } finally {
                synchronized(this@PeerDiscover) {
                    lookupInProgress = false
                    nextLookupTime = System.currentTimeMillis() + if (found) LOOKUP_INTERVAL_MILLIS else RETRY_INTERVAL_MILLIS
                }
            }

            // With no known peers nothing else asks for a lookup again, so once the cooldown
            // expires the peer group is asked to connect, which looks up again only if it is
            // still running and still has no peers
            if (!found) {
                delay(RETRY_INTERVAL_MILLIS)
                peerAddressManager.listener?.onAddAddress()
            }
        }
    }

    private fun lookupSeeds(dnsList: List<String>): Boolean {
        // A broken proxy setting must not fall back to the local resolver, the retry covers it
        val socksProxy = try {
            NetworkUtils.requireSocksProxyAddress()
        } catch (e: IOException) {
            logger.warning("Skip DNS seed lookup: ${e.message}")
            return false
        }
        var found = false

        dnsList.forEach { host ->
            try {
                // Behind a SOCKS proxy (Tor) a local lookup would tell the network which
                // coin the wallet syncs, so the seed is resolved by the proxy instead
                val ips = if (socksProxy != null) {
                    listOfNotNull(NetworkUtils.resolveThroughSocks(host, socksProxy))
                } else {
                    InetAddress
                        .getAllByName(host)
                        .filter { it !is Inet6Address }
                        .map { it.hostAddress }
                }

                logger.info("Fetched ${ips.size} peer addresses from host: $host")
                peerAddressManager.addIps(ips)
                found = found || ips.isNotEmpty()
            } catch (e: UnknownHostException) {
                logger.warning("Cannot look up host: $host")
            } catch (e: IOException) {
                logger.warning("Cannot look up host: $host, ${e.message}")
            }
        }

        return found
    }

    companion object {
        private const val LOOKUP_INTERVAL_MILLIS = 60_000L
        private const val RETRY_INTERVAL_MILLIS = 5_000L
    }
}
