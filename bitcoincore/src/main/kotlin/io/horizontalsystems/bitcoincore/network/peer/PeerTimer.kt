package io.horizontalsystems.bitcoincore.network.peer

import java.util.concurrent.TimeUnit

class PeerTimer {

    private val maxIdleTime = TimeUnit.SECONDS.toMillis(60)
    private val pingTimeout = TimeUnit.SECONDS.toMillis(5)

    private var messageLastReceivedTime: Long? = null
    private var lastPingTime: Long? = null

    fun check() {
        lastPingTime?.let {
            if (System.currentTimeMillis() - it > pingTimeout) {
                throw Error.Timeout(pingTimeout)
            }
        }

        messageLastReceivedTime?.let {
            if (lastPingTime == null && System.currentTimeMillis() - it > maxIdleTime) {
                throw Error.Idle()
            }
        }
    }

    fun pingSent() {
        lastPingTime = System.currentTimeMillis()
    }

    fun restart() {
        messageLastReceivedTime = System.currentTimeMillis()
        lastPingTime = null
    }

    open class Error(message: String) : Exception(message) {
        class Idle: Error("Idle")
        class Timeout(time: Long) : Error("No response within $time milliseconds")
    }

}
