package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.BitcoinKit.NetworkType
import io.horizontalsystems.bitcoinkit.models.FeeRate
import io.reactivex.Observable

class ApiFeeRate(networkType: NetworkType) {
    private val apiManager = ApiManager("https://ipfs.horizontalsystems.xyz")
    private val resource = when (networkType) {
        NetworkType.MainNet -> "BTC"
        NetworkType.TestNet -> "BTC/testnet"
        NetworkType.RegTest -> "BTC/regtest"
        NetworkType.MainNetBitCash -> "BCH"
        NetworkType.TestNetBitCash -> "BCH/testnet"
    }

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
