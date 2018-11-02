package io.horizontalsystems.bitcoinkit.managers

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import io.horizontalsystems.bitcoinkit.BitcoinKit.NetworkType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import java.net.URL

data class ApiAddressTxResponse(val totalCount: Int, val page: Int, val pageSize: Int, val list: List<BlockResponse>)

interface IApiRequester {
    fun requestTransactions(address: String, page: Int): Flowable<ApiAddressTxResponse>
}

class ApiRequesterBtcCom(networkType: NetworkType) : IApiRequester {

    private val urls = mapOf(
            NetworkType.MainNet to "https://chain.api.btc.com/v3",
            NetworkType.MainNetBitCash to "https://bch-chain.api.btc.com/v3",
            NetworkType.TestNet to "https://tchain.api.btc.com/v3",
            NetworkType.TestNetBitCash to "https://bch-tchain.api.btc.com/v3",
            NetworkType.RegTest to "N/A"
    )

    private var url = urls[networkType]

    override fun requestTransactions(address: String, page: Int): Flowable<ApiAddressTxResponse> {
        return Flowable.create<ApiAddressTxResponse>({ emitter ->

            getAsJsonObject("$url/address/$address/tx?page=$page").let { jsonResponse ->

                val list = mutableListOf<BlockResponse>()
                var totalCount = 0
                var page = 0
                var pageSize = 1

                if (jsonResponse.get("data").isObject) {
                    val data = jsonResponse.get("data").asObject()

                    for (tx in data["list"].asArray()) {
                        val txObject = tx.asObject()
                        val blockHash = txObject["block_hash"].asString()
                        val blockHeight = txObject["block_height"].asInt()
                        if (blockHeight > 0) {
                            list.add(BlockResponse(blockHash, blockHeight))
                        }
                    }
                    totalCount = data["total_count"].asInt()
                    page = data["page"].asInt()
                    pageSize = data["pagesize"].asInt()
                }

                val txResponse = ApiAddressTxResponse(
                        totalCount = totalCount,
                        page = page,
                        pageSize = pageSize,
                        list = list)

                emitter.onNext(txResponse)
            }
            emitter.onComplete()
        }, BackpressureStrategy.LATEST)
                .flatMap { response ->
                    val emptyBlockHashes = response.list.filter { it.hash.isBlank() }.toList()

                    if (emptyBlockHashes.isEmpty()) {
                        Flowable.just(response)
                    } else {
                        reloadEmptyHashes(response, emptyBlockHashes)
                    }
                }
    }

    private fun reloadEmptyHashes(txResponse: ApiAddressTxResponse, listOfEmptyBlockHashes: List<BlockResponse>): Flowable<ApiAddressTxResponse> {

        return Flowable.create({ emitter ->

            val listOfHeights = listOfEmptyBlockHashes.map { it.height }.distinct().joinToString(",")

            getAsJsonObject("$url/block/$listOfHeights").let { jsonResponse ->

                val list = mutableListOf<BlockResponse>()

                if (jsonResponse.get("data").isObject) {
                    val data = jsonResponse.get("data").asObject()
                    list.add(BlockResponse(data["hash"].asString(), data["height"].asInt()))
                } else if (jsonResponse.get("data").isArray) {
                    jsonResponse.get("data").asArray().forEach {
                        val block = it.asObject()
                        list.add(BlockResponse(block["hash"].asString(), block["height"].asInt()))
                    }
                }
                val newList = txResponse.list.minus(listOfEmptyBlockHashes).plus(list)

                val newTxResponse = ApiAddressTxResponse(
                        totalCount = txResponse.totalCount,
                        page = txResponse.page,
                        pageSize = txResponse.pageSize,
                        list = newList)

                emitter.onNext(newTxResponse)
            }
            emitter.onComplete()
        }, BackpressureStrategy.LATEST)
    }

    private fun getAsJsonObject(url: String): JsonObject {
        return URL(url).openConnection().apply {
            connectTimeout = 3000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
        }.getInputStream().use {
            Json.parse(it.bufferedReader()).asObject()
        }
    }

}
