package io.horizontalsystems.bitcoincore.apisync

import com.eclipsesource.json.JsonValue
import io.horizontalsystems.bitcoincore.apisync.blockchair.IBlockHashFetcher
import io.horizontalsystems.bitcoincore.apisync.model.AddressItem
import io.horizontalsystems.bitcoincore.apisync.model.TransactionItem
import io.horizontalsystems.bitcoincore.core.IApiTransactionProvider
import io.horizontalsystems.bitcoincore.managers.ApiManager
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class BlockchainComApi(
    transactionApiUrl: String,
    private val blockHashFetcher: IBlockHashFetcher
) : IApiTransactionProvider {

    private val transactionsApiManager = ApiManager(transactionApiUrl)

    private fun getTransactions(addresses: List<String>, offset: Int = 0): List<TransactionResponse> {
        val joinedAddresses = addresses.joinToString("|")
        val json = requestInQueue(transactionsApiManager, "multiaddr?active=$joinedAddresses&n=$paginationLimit&offset=$offset").asObject()

        val transactionsArray = json["txs"].asArray()
        return transactionsArray.map { transactionJson ->
            val transaction = transactionJson.asObject()
            val outputs = transaction["out"].asArray().map { outputJson ->
                val output = outputJson.asObject()

                TransactionOutputResponse(
                    output["script"].asString(),
                    output["addr"]?.asString()
                )
            }
            TransactionResponse(
                if (transaction["block_height"].isNumber) transaction["block_height"].asInt() else null,
                outputs
            )

        }
    }

    private fun fetchTransactionsChunk(addresses: List<String>, stopHeight: Int?, offset: Int = 0): List<TransactionResponse> {
        val transactions = getTransactions(addresses, offset)

        val filteredTransactions = transactions.filter { transaction ->
            if (transaction.blockHeight != null && stopHeight != null) {
                transaction.blockHeight > stopHeight
            } else {
                true
            }
        }

        if (filteredTransactions.size < paginationLimit) {
            return filteredTransactions
        }

        val nextTransactions = fetchTransactionsChunk(addresses, stopHeight, offset + paginationLimit)
        return filteredTransactions + nextTransactions
    }

    private fun fetchTransactions(allAddresses: List<String>, stopHeight: Int?): List<TransactionResponse> {
        val transactions = mutableListOf<TransactionResponse>()
        for (chunk in allAddresses.chunked(addressesLimit)) {
            transactions += fetchTransactionsChunk(chunk, stopHeight)
        }
        return transactions
    }

    override fun transactions(addresses: List<String>, stopHeight: Int?): List<TransactionItem> {
        val transactions = fetchTransactions(addresses, stopHeight)
        val blockHeights = transactions.mapNotNull { it.blockHeight }.distinct()

        if (blockHeights.isEmpty()) return listOf()

        val hashesMap = blockHashFetcher.fetch(blockHeights)

        val items = transactions.mapNotNull { response ->
            val blockHeight = response.blockHeight ?: return@mapNotNull null
            val blockHash = hashesMap[response.blockHeight] ?: return@mapNotNull null

            TransactionItem(
                blockHash = blockHash,
                blockHeight = blockHeight,
                addressItems = response.outputs.map {
                    AddressItem(it.script, it.address)
                }.toMutableList()
            )

        }

        return items
    }

    data class TransactionResponse(
        val blockHeight: Int?,
        val outputs: List<TransactionOutputResponse>
    )

    data class TransactionOutputResponse(
        val script: String,
        val address: String?
    )

    companion object {
        private const val paginationLimit = 100
        private const val addressesLimit = 50

        private val executor = Executors.newSingleThreadExecutor()

        fun requestInQueue(apiManager: ApiManager, path: String): JsonValue {
            val callable = Callable {
                Thread.sleep(500)
                apiManager.doOkHttpGet(path)
            }

            return executor.submit(callable).get()
        }

    }

}
