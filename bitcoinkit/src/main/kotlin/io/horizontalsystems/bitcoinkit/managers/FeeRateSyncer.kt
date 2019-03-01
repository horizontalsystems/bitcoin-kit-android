package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

class FeeRateSyncer(private val storage: IStorage, private val apiFeeRate: ApiFeeRate) {

    private var disposable: Disposable? = null

    fun sync() {
        disposable = apiFeeRate.getFeeRate()
                .onErrorResumeNext(Observable.empty())
                .subscribe {
                    storage.setFeeRate(it)
                }
    }
}
