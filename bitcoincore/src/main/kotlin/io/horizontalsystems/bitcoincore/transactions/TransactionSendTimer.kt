package io.horizontalsystems.bitcoincore.transactions

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class TransactionSendTimer(private val period: Long) {

    interface Listener {
        fun onTimePassed()
    }

    var listener: Listener? = null

    private var executor = Executors.newSingleThreadScheduledExecutor()
    private var task: ScheduledFuture<*>? = null

    @Synchronized
    fun startIfNotRunning() {
        if (task == null) {
            task = executor.scheduleAtFixedRate({ listener?.onTimePassed() }, period, period, TimeUnit.SECONDS)
        }
    }

    @Synchronized
    fun stop() {
        task?.let {
            it.cancel(true)
            task = null
        }
    }

}
