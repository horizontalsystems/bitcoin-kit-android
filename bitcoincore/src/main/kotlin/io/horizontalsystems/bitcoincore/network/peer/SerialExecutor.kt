package io.horizontalsystems.bitcoincore.network.peer

import java.util.ArrayDeque
import java.util.concurrent.Executor

/**
 * Runs tasks on the backing [executor] strictly one at a time, in submission order.
 *
 * Every peer connection shares one thread pool for outgoing messages. Without this
 * wrapper two messages queued back-to-back for the same socket (for example
 * `filterload` followed by `getdata`) can be written by two pool threads concurrently,
 * so the peer may receive them out of order and answer `getdata` using a stale bloom filter.
 */
class SerialExecutor(private val executor: Executor) : Executor {

    private val tasks = ArrayDeque<Runnable>()
    private var active: Runnable? = null

    @Synchronized
    override fun execute(task: Runnable) {
        tasks.offer(Runnable {
            try {
                task.run()
            } finally {
                scheduleNext()
            }
        })

        if (active == null) {
            scheduleNext()
        }
    }

    @Synchronized
    private fun scheduleNext() {
        active = tasks.poll()
        active?.let { executor.execute(it) }
    }
}
