package io.horizontalsystems.bitcoincore.network.peer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SerialExecutorTest {

    @Test
    fun `tasks run one at a time in submission order on a shared pool`() {
        val pool = Executors.newCachedThreadPool()
        val serial = SerialExecutor(pool)
        val count = 200
        val order = Collections.synchronizedList(mutableListOf<Int>())
        val done = CountDownLatch(count)
        var running = 0
        var maxRunning = 0
        val lock = Any()

        repeat(count) { i ->
            serial.execute {
                synchronized(lock) {
                    running += 1
                    maxRunning = maxOf(maxRunning, running)
                }
                // first task is slow so the rest pile up behind it
                if (i == 0) Thread.sleep(50)
                order.add(i)
                synchronized(lock) { running -= 1 }
                done.countDown()
            }
        }

        assertEquals(true, done.await(10, TimeUnit.SECONDS))
        assertEquals((0 until count).toList(), order.toList())
        assertEquals(1, maxRunning)
        pool.shutdown()
    }

    @Test
    fun `a failing task does not block the tasks behind it`() {
        val pool = Executors.newCachedThreadPool()
        val serial = SerialExecutor(pool)
        val done = CountDownLatch(1)

        serial.execute { throw IllegalStateException("boom") }
        serial.execute { done.countDown() }

        assertEquals(true, done.await(5, TimeUnit.SECONDS))
        pool.shutdown()
    }
}
