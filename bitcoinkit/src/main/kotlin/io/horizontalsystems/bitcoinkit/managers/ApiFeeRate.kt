package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.models.FeeRate
import io.reactivex.Observable

class ApiFeeRate(private val resource: String) {
    private val apiManager = ApiManager("https://ipfs.horizontalsystems.xyz")

    fun getFeeRate(): Observable<FeeRate> {
        return Observable.create { subscriber ->
            try {
                val json = apiManager.getJson("ipns/Qmd4Gv2YVPqs6dmSy1XEq7pQRSgLihqYKL2JjK7DMUFPVz/io-hs/data/blockchain/$resource/estimatefee/index.json")
                val rate = FeeRate(
                        json["low_priority"].asString(),
                        json["medium_priority"].asString(),
                        json["high_priority"].asString(),
                        json["date"].asLong()
                )

                subscriber.onNext(rate)
                subscriber.onComplete()
            } catch (e: Exception) {
                subscriber.onError(e)
            }
        }
    }
}
