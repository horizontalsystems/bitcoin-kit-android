package io.horizontalsystems.bitcoincore.managers

import com.eclipsesource.json.JsonValue
import io.horizontalsystems.bitcoincore.core.IInitialSyncApi
import java.lang.Integer.min
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class BlockchainComApi(transactionApiUrl: String, blocksApiUrl: String) : IInitialSyncApi {

    private val transactionsApiManager = ApiManager(transactionApiUrl)
    private val blocksApiManager = ApiManager(blocksApiUrl)

    private fun getTransactions(addresses: List<String>, offset: Int = 0): List<TransactionResponse> {
        val joinedAddresses = addresses.joinToString("|")
        val json = requestInQueue(transactionsApiManager, "multiaddr?active=$joinedAddresses&n=${paginationLimit}&offset=$offset").asObject()

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
                transaction["block_height"].asInt(),
                outputs
            )
        }
    }

    private fun blocks(heights: List<Int>): List<BlockResponse> {
        val joinedHeights = heights.sorted().joinToString(",") { it.toString() }
        val blocks = blocksApiManager.doOkHttpGet(false, "hashes?numbers=$joinedHeights").asArray()

        return blocks.map { blockJson ->
            val block = blockJson.asObject()

            BlockResponse(
                block["number"].asInt(),
                block["hash"].asString()
            )
        }
    }

    private fun getItems(addresses: List<String>, offset: Int): List<TransactionItem> {
        val transactionResponses = getTransactions(addresses, offset)
        if (transactionResponses.isEmpty()) return listOf()

        val blockHeights = transactionResponses.map { it.blockHeight }.toSet().toList()
        val blocks = blocks(blockHeights)

        return transactionResponses.mapNotNull { transactionResponse ->
            val blockHash = blocks.firstOrNull { it.height == transactionResponse.blockHeight } ?: return@mapNotNull null

            val outputs = transactionResponse.outputs.mapNotNull { output ->
                val address = output.address ?: return@mapNotNull null

                TransactionOutputItem(output.script, address)
            }

            TransactionItem(blockHash.hash, transactionResponse.blockHeight, outputs)
        }
    }

    private fun getItemsForChunk(addressChunk: List<String>, offset: Int = 0): List<TransactionItem> {
        val chunkItems = getItems(addressChunk, offset)

        if (chunkItems.size < paginationLimit) {
            return chunkItems
        }

        return chunkItems + getItemsForChunk(addressChunk, offset + paginationLimit)
    }

    private fun getAllItems(allAddresses: List<String>, index: Int = 0): List<TransactionItem> {
        val startIndex = index * addressesLimit
        if (startIndex > allAddresses.size) return listOf()

        val endIndex = min(allAddresses.size, (index + 1) * addressesLimit)
        val chunk = allAddresses.subList(startIndex, endIndex)

        return getItemsForChunk(chunk) + getAllItems(allAddresses, index + 1)
    }

    override fun getTransactions(addresses: List<String>): List<TransactionItem> {
        return getAllItems(addresses)
    }

    data class TransactionResponse(
        val blockHeight: Int,
        val outputs: List<TransactionOutputResponse>
    )

    data class TransactionOutputResponse(
        val script: String,
        val address: String?
    )

    data class BlockResponse(
        val height: Int,
        val hash: String
    )

    companion object {
        private const val paginationLimit = 100
        private const val addressesLimit = 50

        private val executor = Executors.newSingleThreadExecutor()

        fun requestInQueue(apiManager: ApiManager, path: String): JsonValue {
            val callable = Callable {
                Thread.sleep(500)
                apiManager.doOkHttpGet(false, path)
            }

            return executor.submit(callable).get()
        }

    }

}
