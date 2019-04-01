package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.bitcoinkit.BitcoinKit.NetworkType
import io.horizontalsystems.bitcoinkit.models.FeeRate
import io.reactivex.Maybe

class ApiFeeRate(networkType: BitcoinKit.NetworkType) {
    private val apiManager = ApiManager("https://ipfs.io")
    private val coinCode = when (networkType) {
        NetworkType.MainNet, NetworkType.TestNet, NetworkType.RegTest -> "BTC"
        NetworkType.MainNetBitCash, NetworkType.TestNetBitCash -> "BCH"
    }

    fun getFeeRate(): Maybe<FeeRate> {
        return Maybe.create { subscriber ->
            try {
                val json = apiManager.getJson("ipns/QmXTJZBMMRmBbPun6HFt3tmb3tfYF2usLPxFoacL7G5uMX/blockchain/estimatefee/index.json")

                val btcRates = json.get("rates").asObject().get(coinCode).asObject()
                val rate = FeeRate().apply {
                    lowPriority = btcRates["low_priority"].asDouble()
                    mediumPriority = btcRates["medium_priority"].asDouble()
                    highPriority = btcRates["high_priority"].asDouble()
                    date = json["time"].asLong()
                    dateStr = json["time_str"].asString()
                }

                subscriber.onSuccess(rate)
                subscriber.onComplete()
            } catch (e: Exception) {
                subscriber.onError(e)
            }
        }
    }
}
