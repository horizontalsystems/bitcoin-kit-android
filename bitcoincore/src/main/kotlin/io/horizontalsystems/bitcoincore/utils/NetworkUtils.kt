package io.horizontalsystems.bitcoincore.utils

import io.horizontalsystems.bitcoincore.extensions.toHexString
import java.io.DataInputStream
import java.io.IOException
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

    fun socksProxyAddress(): InetSocketAddress? = try {
        requireSocksProxyAddress()
    } catch (e: IOException) {
        null
    }

    /**
     * Returns null when no SOCKS proxy is set, and throws when one is set but incomplete or
     * invalid, so callers that must not bypass the proxy can tell the two apart.
     */
    fun requireSocksProxyAddress(): InetSocketAddress? {
        val socksProxyHost = System.getProperty("socksProxyHost")
        val socksProxyPortValue = System.getProperty("socksProxyPort")
        if (socksProxyHost == null && socksProxyPortValue == null) return null

        val socksProxyPort = socksProxyPortValue?.toIntOrNull()?.takeIf { it in 0..65535 }
        if (socksProxyHost == null || socksProxyPort == null) {
            throw IOException("Invalid SOCKS proxy configuration: host=$socksProxyHost, port=$socksProxyPortValue")
        }

        return InetSocketAddress.createUnresolved(socksProxyHost, socksProxyPort)
    }

    fun createSocket(): Socket {
        val socketAddress = socksProxyAddress()

        return if (socketAddress != null) {
            val proxy = Proxy(Proxy.Type.SOCKS, socketAddress)
            Socket(proxy)
        } else {
            Socket()
        }
    }

    /**
     * Resolves [host] to one IPv4 address through Tor's SOCKS RESOLVE extension (command 0xF0),
     * so the lookup never reaches the local DNS resolver. Only Tor implements it.
     */
    fun resolveThroughSocks(host: String, proxy: InetSocketAddress, timeoutMillis: Int = 30_000): String? {
        val hostBytes = host.toByteArray(Charsets.US_ASCII)
        require(hostBytes.size <= 255) { "Host name too long: $host" }

        Socket().use { socket ->
            socket.connect(InetSocketAddress(proxy.hostString, proxy.port), timeoutMillis)
            socket.soTimeout = timeoutMillis
            val output = socket.getOutputStream()
            val input = DataInputStream(socket.getInputStream())

            output.write(byteArrayOf(0x05, 0x01, 0x00))
            output.flush()
            val greeting = ByteArray(2).also { input.readFully(it) }
            if (greeting[0] != 0x05.toByte() || greeting[1] != 0x00.toByte()) return null

            output.write(byteArrayOf(0x05, 0xF0.toByte(), 0x00, 0x03, hostBytes.size.toByte()) + hostBytes + byteArrayOf(0x00, 0x00))
            output.flush()

            val header = ByteArray(4).also { input.readFully(it) }
            if (header[1] != 0x00.toByte()) return null

            return when (header[3]) {
                0x01.toByte() -> {
                    val address = ByteArray(4).also { input.readFully(it) }
                    InetAddress.getByAddress(address).hostAddress
                }
                else -> null
            }
        }
    }

}
