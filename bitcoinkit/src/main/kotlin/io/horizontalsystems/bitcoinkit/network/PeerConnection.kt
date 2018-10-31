package io.horizontalsystems.bitcoinkit.network

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.messages.Message
import io.horizontalsystems.bitcoinkit.messages.VersionMessage
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

class PeerConnection(val host: String, private val network: NetworkParameters, private val listener: Listener) : Thread() {

    interface Listener {
        fun onMessage(message: Message)
        fun disconnected(e: Exception? = null)
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

            // add version message to send automatically:
            sendMessage(VersionMessage(0, socket.inetAddress, network))
            // loop:
            while (isRunning) {
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
            logger.log(Level.SEVERE, "Connect timeout exception: ${e.message}", e)
            listener.disconnected(e)
        } catch (e: ConnectException) {
            logger.log(Level.SEVERE, "Connect exception: ${e.message}", e)
            listener.disconnected(e)
        } catch (e: IOException) {
            logger.log(Level.SEVERE, "IOException: ${e.message}", e)
            listener.disconnected(e)
        } catch (e: InterruptedException) {
            logger.log(Level.SEVERE, "Peer connection thread interrupted.")
            listener.disconnected()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Peer connection exception.", e)
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
