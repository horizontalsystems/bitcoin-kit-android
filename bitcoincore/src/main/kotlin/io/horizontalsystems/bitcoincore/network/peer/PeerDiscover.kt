package io.horizontalsystems.bitcoincore.network.peer

import io.horizontalsystems.bitcoincore.core.IPeerAddressManager
import io.horizontalsystems.bitcoincore.utils.NetworkUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.logging.Logger

class PeerDiscover(private val peerAddressManager: IPeerAddressManager) {

    private val logger = Logger.getLogger("PeerDiscover")

    fun lookup(dnsList: List<String>) {
        logger.info("Lookup peers from DNS seed...")

        // todo: launch coroutines for each dns resolve
        GlobalScope.launch {
            val socksProxy = NetworkUtils.socksProxyAddress()

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
                } catch (e: UnknownHostException) {
                    logger.warning("Cannot look up host: $host")
                } catch (e: IOException) {
                    logger.warning("Cannot look up host: $host, ${e.message}")
                }
            }
        }
    }
}
