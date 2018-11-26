package io.horizontalsystems.bitcoinkit.network.peer

import io.horizontalsystems.bitcoinkit.exceptions.BitcoinException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import java.util.logging.Logger

class PeerDiscover {

    private val log = Logger.getLogger("PeerDiscover")

    /**
     * Lookup bitcoin peers by DNS seed.
     *
     * @return InetAddress[] contains 1~N peers.
     * @throws BitcoinException If lookup failed.
     */
    fun lookup(dnsList: Array<String>): Array<String> {
        log.info("Lookup peers from DNS seed...")

        val ips: MutableList<String> = ArrayList()

        dnsList.forEach { host ->
            try {
                val addresses = InetAddress.getAllByName(host)
                for (address in addresses) {
                    if (address is InetAddress) {
                        ips.add(address.hostAddress)
                    }
                }
            } catch (e: UnknownHostException) {
                log.warning("Cannot look up host: $host")
            }
        }

        if (ips.isEmpty()) {
            throw Exception("Cannot lookup pears from all DNS seeds.")
        }

        log.info(ips.size.toString() + " peers found.")

        return ips.toTypedArray()
    }
}
