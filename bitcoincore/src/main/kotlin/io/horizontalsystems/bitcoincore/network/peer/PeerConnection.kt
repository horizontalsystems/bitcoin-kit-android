package io.horizontalsystems.bitcoincore.network.peer

import io.horizontalsystems.bitcoincore.io.BitcoinInput
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.messages.NetworkMessageParser
import io.horizontalsystems.bitcoincore.network.messages.NetworkMessageSerializer
import io.horizontalsystems.bitcoincore.utils.NetworkUtils
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ExecutorService
import java.util.logging.Logger

class PeerConnection(
        private val host: String,
        private val network: Network,
        private val listener: Listener,
        private val sendingExecutor: ExecutorService,
        private val networkMessageParser: NetworkMessageParser,
        private val networkMessageSerializer: NetworkMessageSerializer)
    : Runnable {

    interface Listener {
        fun socketConnected(address: InetAddress)
        fun disconnected(e: Exception? = null)
        fun onTimePeriodPassed() // didn't find better name
        fun onMessage(message: IMessage)
    }

    private val socket = NetworkUtils.createSocket()

    private val logger = Logger.getLogger("Peer[$host]")
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var disconnectError: Exception? = null

    @Volatile
    private var isRunning = false

    override fun run() {
        isRunning = true
        // connect:
        try {
            socket.connect(InetSocketAddress(host, network.port), 10000)
            socket.soTimeout = 10000

            outputStream = socket.getOutputStream()
            val inputStream = socket.getInputStream()
            val bitcoinInput = BitcoinInput(inputStream)

            logger.info("Socket $host connected.")

            listener.socketConnected(socket.inetAddress)

            this.inputStream = inputStream
            // loop:
            while (isRunning) {
                listener.onTimePeriodPassed()

                Thread.sleep(1000)

                // try receive message:
                while (isRunning && inputStream.available() > 0) {
                    val parsedMsg = networkMessageParser.parseMessage(bitcoinInput)
                    logger.info("<= $parsedMsg")
                    listener.onMessage(parsedMsg)
                }
            }
        } catch (e: Exception) {
            close(e)
        } finally {
            outputStream?.close()
            outputStream = null

            inputStream?.close()
            inputStream = null

            listener.disconnected(disconnectError)
        }
    }

    @Synchronized
    fun close(error: Exception?) {
        disconnectError = error
        isRunning = false
    }

    @Synchronized
    fun sendMessage(message: IMessage) {
        sendingExecutor.execute {
            if (isRunning) {
                try {
                    logger.info("=> $message")
                    outputStream?.write(networkMessageSerializer.serialize(message))
                } catch (e: Exception) {
                    close(e)
                }
            }
        }
    }

}
