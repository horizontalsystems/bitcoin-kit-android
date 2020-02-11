package io.horizontalsystems.bitcoincore.utils

import io.horizontalsystems.bitcoincore.extensions.toHexString
import java.net.*

object NetworkUtils {

    fun getLocalInetAddress(): InetAddress {
        try {
            return InetAddress.getLocalHost()
        } catch (e: UnknownHostException) {
            throw RuntimeException(e)
        }
    }

    fun getIPv6(inetAddr: InetAddress): ByteArray {
        val ip = inetAddr.address
        if (ip.size == 16) {
            return ip
        }

        if (ip.size == 4) {
            val ipv6 = ByteArray(16)
            ipv6[10] = -1
            ipv6[11] = -1
            System.arraycopy(ip, 0, ipv6, 12, 4)
            return ipv6
        }

        throw RuntimeException("Bad IP: " + ip.toHexString())
    }

    fun createSocket(): Socket {

        val socksProxyHost = System.getProperty("socksProxyHost")
        val socksProxyPort = System.getProperty("socksProxyPort")?.toIntOrNull()

        if (socksProxyHost != null && socksProxyPort != null) {
            val socketAddress = InetSocketAddress.createUnresolved(socksProxyHost, socksProxyPort)
            val proxy = Proxy(Proxy.Type.SOCKS, socketAddress)
            return Socket(proxy)
        } else {
            return Socket()
        }
    }
}
