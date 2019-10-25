package io.horizontalsystems.bitcoincore.network.peer

import io.horizontalsystems.bitcoincore.core.IPeerAddressManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.logging.Logger

class PeerDiscover(private val peerAddressManager: IPeerAddressManager) {

    private val logger = Logger.getLogger("PeerDiscover")

    fun lookup(dnsList: List<String>) {
        logger.info("Lookup peers from DNS seed...")

        // todo: launch coroutines for each dns resolve
        GlobalScope.launch {
            val ips = mutableListOf<String>()
            dnsList.forEach { host ->
                try {
                    InetAddress.getAllByName(host).forEach {
                        ips.add(it.hostAddress)
                    }
                } catch (e: UnknownHostException) {
                    logger.warning("Cannot look up host: $host")
                }
            }
            peerAddressManager.addIps(ips)
        }
    }
}
