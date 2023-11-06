package io.horizontalsystems.bitcoincore.apisync

import io.horizontalsystems.bitcoincore.apisync.blockchair.IBlockHashFetcher
import io.horizontalsystems.bitcoincore.managers.ApiManager

class HsBlockHashFetcher(url: String) : IBlockHashFetcher {
    private val apiManager = ApiManager(url)

    override fun fetch(heights: List<Int>): Map<Int, String> {
        val joinedHeights = heights.sorted().joinToString(",") { it.toString() }
        val blocks = apiManager.doOkHttpGet("hashes?numbers=$joinedHeights").asArray()

        return blocks.associate { blockJson ->
            val block = blockJson.asObject()
            Pair(
                block["number"].asInt(),
                block["hash"].asString()
            )
        }
    }
}
