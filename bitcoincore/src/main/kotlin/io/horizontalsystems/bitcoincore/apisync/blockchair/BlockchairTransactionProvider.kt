package io.horizontalsystems.bitcoincore.apisync.blockchair

import io.horizontalsystems.bitcoincore.apisync.model.TransactionItem
import io.horizontalsystems.bitcoincore.core.IApiTransactionProvider
import io.horizontalsystems.bitcoincore.managers.ApiManagerException

class BlockchairTransactionProvider(
    val blockchairApi: BlockchairApi,
    private val blockHashFetcher: IBlockHashFetcher
) : IApiTransactionProvider {

    private fun fillBlockHashes(items: List<TransactionItem>): List<TransactionItem> {
        val heights = items.map { it.blockHeight }.distinct()
        val hashesMap = blockHashFetcher.fetch(heights)

        // Every item refers to a confirmed block, so every height must resolve. Failing
        // the sync keeps it retryable; silently dropping items would lose transactions.
        val missingHeights = heights.filter { it !in hashesMap }
        if (missingHeights.isNotEmpty()) {
            throw ApiManagerException.Other("Missing block hashes for heights: $missingHeights")
        }

        return items.map { item ->
            item.copy(blockHash = hashesMap.getValue(item.blockHeight))
        }
    }

    override fun transactions(addresses: List<String>, stopHeight: Int?): List<TransactionItem> {
        val items = blockchairApi.transactions(addresses, stopHeight)
        return fillBlockHashes(items)
    }

}
