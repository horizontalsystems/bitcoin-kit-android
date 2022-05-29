package io.horizontalsystems.bitcoincore.network

import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object ExecutorsUtil {
    /*val executorService = Executors.newFixedThreadPool(30)
    val peerThreadPool = Executors.newFixedThreadPool(100)*/

    private val executorService = ThreadPoolExecutor(30, 30,
    0L, TimeUnit.MILLISECONDS,
    LinkedBlockingQueue<Runnable>()
    )
    private val peerThreadPool = ThreadPoolExecutor(200, 200,
        0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue<Runnable>()
    )

    fun getExecutorService():ThreadPoolExecutor {
        return executorService
    }

    fun getPeerThreadPool():ThreadPoolExecutor {
        return peerThreadPool
    }
}