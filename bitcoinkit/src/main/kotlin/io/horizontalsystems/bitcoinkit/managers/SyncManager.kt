package io.horizontalsystems.bitcoinkit.managers

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

class SyncManager(private val connectionManager: ConnectionManager, private val feeRateSyncer: FeeRateSyncer) {

    private var disposable: Disposable? = null

    fun start() {
        if (disposable != null && disposable?.isDisposed == false) {
            return
        }

        disposable = Observable
                .interval(0, 3, TimeUnit.MINUTES)
                .subscribe {
                    syncFeeRate()
                }
    }

    fun stop() {
        disposable?.dispose()
        disposable = null
    }

    private fun syncFeeRate() {
        if (connectionManager.isOnline) {
            feeRateSyncer.sync()
        }
    }
}
