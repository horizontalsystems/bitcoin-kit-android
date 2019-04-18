package io.horizontalsystems.bitcoincore.network.peer

import io.horizontalsystems.bitcoincore.exceptions.BitcoinException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import java.util.logging.Logger

class PeerDiscover(private val peerAddressManager: PeerAddressManager) {

    private val logger = Logger.getLogger("PeerDiscover")

    /**
     * Lookup bitcoin peers by DNS seed.
     *
     * @return InetAddress[] contains 1~N peers.
     * @throws BitcoinException If lookup failed.
     */
    fun lookup(dnsList: Array<String>) {
        logger.info("Lookup peers from DNS seed...")

        val ips: MutableList<String> = ArrayList()

        dnsList.forEach { host ->
            try {
                InetAddress.getAllByName(host).forEach {
                    ips.add(it.hostAddress)
                }
            } catch (e: UnknownHostException) {
                logger.warning("Cannot look up host: $host")
            }
        }

        logger.info(ips.size.toString() + " peers found.")

        peerAddressManager.addIps(ips.toTypedArray())
    }
}
