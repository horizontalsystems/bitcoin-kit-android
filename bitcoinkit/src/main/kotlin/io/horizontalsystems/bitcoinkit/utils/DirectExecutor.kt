package io.horizontalsystems.bitcoinkit.utils

import java.util.concurrent.Executor

class DirectExecutor : Executor {

    override fun execute(command: Runnable) {
        command.run()
    }

}
