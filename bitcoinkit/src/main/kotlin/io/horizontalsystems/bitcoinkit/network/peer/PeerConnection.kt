package io.horizontalsystems.bitcoinkit.network.peer

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.bitcoinkit.network.messages.Message
import java.io.IOException
import java.net.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

class PeerConnection(private val host: String, private val network: Network, private val listener: Listener) : Thread() {

    interface Listener {
        fun socketConnected(address: InetAddress)
        fun disconnected(e: Exception? = null)
        fun onTimePeriodPassed() // didn't find better name
        fun onMessage(message: Message)
    }

    private val logger = Logger.getLogger("Peer[$host]")
    private val sendingQueue: BlockingQueue<Message> = ArrayBlockingQueue(100)
    private val socket = Socket()
    private var disconnectError: Exception? = null

    @Volatile
    private var isRunning = false

    // initialize:
    init {
        isDaemon = true
    }

    override fun run() {
        isRunning = true
        // connect:
        try {
            socket.connect(InetSocketAddress(host, network.port), 10000)
            socket.soTimeout = 10000

            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            logger.info("Socket $host connected.")

            listener.socketConnected(socket.inetAddress)
            // loop:
            while (isRunning) {
                listener.onTimePeriodPassed()

                // try get message to send:
                val msg = sendingQueue.poll(1, TimeUnit.SECONDS)
                if (isRunning && msg != null) {
                    // send message:
                    logger.info("=> " + msg.toString())
                    output.write(msg.toByteArray(network))
                }

                // try receive message:
                while (isRunning && input.available() > 0) {
                    val inputStream = BitcoinInput(input)
                    val parsedMsg = Message.Builder.parseMessage<Message>(inputStream, network)
                    logger.info("<= $parsedMsg")
                    listener.onMessage(parsedMsg)
                }
            }

            listener.disconnected(disconnectError)
        } catch (e: SocketTimeoutException) {
            logger.warning("Socket timeout exception: ${e.message}")
            listener.disconnected(e)
        } catch (e: ConnectException) {
            logger.warning("Connect exception: ${e.message}")
            listener.disconnected(e)
        } catch (e: IOException) {
            logger.warning("IOException: ${e.message}")
            listener.disconnected(e)
        } catch (e: InterruptedException) {
            logger.warning("Peer connection thread interrupted: ${e.message}")
            listener.disconnected()
        } catch (e: Exception) {
            logger.warning("Peer connection exception: ${e.message}")
            listener.disconnected()
        } finally {
            isRunning = false
        }
    }

    fun close(disconnectError: Exception?) {
        this.disconnectError = disconnectError

        isRunning = false
        try {
            join(1000)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, e.message)
        }
    }

    fun sendMessage(message: Message) {
        sendingQueue.add(message)
    }

}
