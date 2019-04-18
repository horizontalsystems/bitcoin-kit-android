package io.horizontalsystems.bitcoincore.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

class MainThreadExecutor : Executor {

    private val uiHandler = Handler(Looper.getMainLooper())

    override fun execute(command: Runnable) {
        uiHandler.post(command)
    }
}
