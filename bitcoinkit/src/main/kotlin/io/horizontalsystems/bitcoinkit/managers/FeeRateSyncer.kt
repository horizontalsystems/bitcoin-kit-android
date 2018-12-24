package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.models.FeeRate
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

class FeeRateSyncer(private val realmFactory: RealmFactory, private val apiFeeRate: ApiFeeRate, private val connectionManager: ConnectionManager? = null) {

    private var timer = Observable.interval(0, 3, TimeUnit.MINUTES)
    private var feeRateDisposable: Disposable? = null
    private var timerDisposable: Disposable? = null

    constructor(realmFactory: RealmFactory, apiFeeRate: ApiFeeRate, timer: Observable<Long>, connectionManager: ConnectionManager) : this(realmFactory, apiFeeRate, connectionManager) {
        this.timer = timer
    }

    fun start() {
        if (timerDisposable != null && timerDisposable?.isDisposed == false) {
            return
        }

        timerDisposable = timer.subscribe {
            if (connectionManager?.isOnline == true && (feeRateDisposable == null || feeRateDisposable?.isDisposed == true)) {
                updateFeeRate()
            }
        }
    }

    fun stop() {
        timerDisposable?.dispose()
        timerDisposable = null
    }

    private fun updateFeeRate() {
        feeRateDisposable = apiFeeRate.getFeeRate()
                .subscribe({ saveFeeRate(it) }, { dispose() })
    }

    private fun saveFeeRate(feeRate: FeeRate) {
        dispose()

        realmFactory.realm.use { realm ->
            realm.executeTransaction {
                realm.insertOrUpdate(feeRate)
            }
        }
    }

    private fun dispose() {
        feeRateDisposable?.dispose()
        feeRateDisposable = null
    }

}
