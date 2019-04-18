package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.models.FeeRate
import io.reactivex.Maybe

class ApiFeeRate(private val coinCode: String) {
    private val apiManager = ApiManager("https://ipfs.horizontalsystems.xyz")

    fun getFeeRate(): Maybe<FeeRate> {
        return Maybe.create { subscriber ->
            try {
                val json = apiManager.getJson("ipns/QmXTJZBMMRmBbPun6HFt3tmb3tfYF2usLPxFoacL7G5uMX/blockchain/estimatefee/index.json")

                val btcRates = json.get("rates").asObject().get(coinCode).asObject()
                val rate = FeeRate(btcRates["low_priority"].asInt(), btcRates["medium_priority"].asInt(), btcRates["high_priority"].asInt(), json["time"].asLong())
                subscriber.onSuccess(rate)
                subscriber.onComplete()
            } catch (e: Exception) {
                subscriber.onError(e)
            }
        }
    }
}
