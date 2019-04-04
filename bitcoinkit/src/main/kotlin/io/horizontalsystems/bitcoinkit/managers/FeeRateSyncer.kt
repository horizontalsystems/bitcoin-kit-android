package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.reactivex.Maybe
import io.reactivex.disposables.Disposable

class FeeRateSyncer(private val storage: IStorage, private val apiFeeRate: ApiFeeRate) {

    private var disposable: Disposable? = null

    fun sync() {
        disposable = apiFeeRate.getFeeRate()
                .onErrorResumeNext(Maybe.empty())
                .subscribe {
                    storage.setFeeRate(it)
                }
    }
}
