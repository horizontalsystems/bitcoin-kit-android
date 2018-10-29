package io.horizontalsystems.bitcoinkit.managers

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import io.reactivex.Observable
import java.net.URL

class ApiManager(private val url: String) {

    fun getBlockHashes(address: String): Observable<List<BlockResponse>> {
        return Observable.create { subscriber ->
            val blocksList = ArrayList<BlockResponse>()
            val addressPath = "${address.substring(0, 3)}/${address.substring(3, 6)}/${address.substring(6)}"
            try {
                getAsJsonObject("$url/btc-regtest/address/$addressPath/index.json")?.let { jsonResponse ->
                    for (block in jsonResponse["blocks"].asArray()) {
                        val blockObject = block.asObject()
                        blocksList.add(BlockResponse(blockObject["hash"].asString(), blockObject["height"].asInt()))
                    }
                }
                subscriber.onNext(blocksList)
                subscriber.onComplete()
            } catch (e: Exception) {
                subscriber.onError(e)
            }
        }
    }

    private fun getAsJsonObject(url: String): JsonObject? {
        return try {
            URL(url).openConnection().apply {
                connectTimeout = 500
                readTimeout = 1000
                setRequestProperty("Accept", "application/json")
            }.getInputStream().use {
                Json.parse(it.bufferedReader()).asObject()
            }
        } catch (ex: Exception) {
            null
        }
    }

}

data class BlockResponse(val hash: String, val height: Int)
