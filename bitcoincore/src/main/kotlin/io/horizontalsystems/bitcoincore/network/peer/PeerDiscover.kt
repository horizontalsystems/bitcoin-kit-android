package io.horizontalsystems.bitcoincore.network.peer

import io.horizontalsystems.bitcoincore.utils.HSLogger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.UnknownHostException

class PeerDiscover(private val peerAddressManager: PeerAddressManager) {

    private val logger = HSLogger("PeerDiscover")

    fun lookup(dnsList: List<String>) {
        logger.i("Lookup peers from DNS seed...")

        // todo: launch coroutines for each dns resolve
        GlobalScope.launch {
            val ips = mutableListOf<String>()
            dnsList.forEach { host ->
                try {
                    InetAddress.getAllByName(host).forEach {
                        ips.add(it.hostAddress)
                    }
                } catch (e: UnknownHostException) {
                    logger.w( "Cannot look up host: $host")
                }
            }
            peerAddressManager.addIps(ips)
        }
    }
}
