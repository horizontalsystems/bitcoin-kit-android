package io.horizontalsystems.bitcoincore.apisync.blockchair

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import io.horizontalsystems.bitcoincore.apisync.model.AddressItem
import io.horizontalsystems.bitcoincore.apisync.model.BlockHeaderItem
import io.horizontalsystems.bitcoincore.apisync.model.TransactionItem
import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.managers.ApiManager
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class BlockchairApi(
    private val chainId: String,
) {
    private val apiManager = ApiManager("https://api.blocksdecoded.com/v1/blockchair")
    private val limit = 10000
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    init {
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
    }

    /**
     * Runs an API request on the shared, spaced executor with bounded retries.
     *
     * All chains of the bitcoin family talk to the same host; restoring a wallet
     * with many coins fires their scans concurrently, which the endpoint answers
     * with transient 404/429 responses. Serializing and spacing the requests keeps
     * the load acceptable, and retrying with backoff rides out the residual
     * failures. When retries are exhausted the error propagates, failing the sync
     * loudly: an error response must never be treated as an empty history, or the
     * affected addresses' transactions would be silently lost.
     */
    private fun <T> request(block: () -> T): T {
        val future = executor.submit(Callable {
            var lastError: Exception? = null
            for (attempt in 0 until maxAttempts) {
                if (attempt > 0) {
                    Thread.sleep(retryBaseDelayMs shl (attempt - 1))
                }
                try {
                    Thread.sleep(requestSpacingMs)
                    return@Callable block()
                } catch (e: InterruptedException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                }
            }
            throw lastError!!
        })
        try {
            return future.get()
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }

    fun transactions(addresses: List<String>, stopHeight: Int?): List<TransactionItem> {
        val transactionItemsMap = mutableMapOf<String, TransactionItem>()

        for (chunk in addresses.chunked(100)) {
            val (addressItems, transactions) = fetchTransactions(chunk, stopHeight)

            for (transaction in transactions) {
                val blockHeight = transaction.blockId ?: continue
                val existing = transactionItemsMap[transaction.hash]

                val transactionItem: TransactionItem
                if (existing != null) {
                    transactionItem = existing
                } else {
                    transactionItem = TransactionItem(
                        blockHash = "",
                        blockHeight = blockHeight,
                        addressItems = mutableListOf()
                    )
                    transactionItemsMap[transaction.hash] = transactionItem
                }
                addressItems.firstOrNull { it.address == transaction.address }?.let { addressItem ->
                    transactionItem.addressItems += addressItem
                }
            }
        }
        return transactionItemsMap.values.toList()
    }

    fun blockHashes(heights: List<Int>): Map<Int, String> {
        val hashesMap = mutableMapOf<Int, String>()

        for (chunk in heights.chunked(10)) {
            val map = fetchBlockHashes(chunk)
            hashesMap += map
        }

        return hashesMap
    }

    fun lastBlockHeader(): BlockHeaderItem {
        val params = "?limit=0"
        val url = "$chainId/stats"
        val response = apiManager.doOkHttpGet(url + params).asObject()
        val data = response.get("data").asObject()

        val height = data["best_block_height"].asInt()
        val hash = data["best_block_hash"].asString()
        val date = data["best_block_time"].asString()
        val timestamp = dateStringToTimestamp(date)

        return BlockHeaderItem(hash.hexToByteArray(), height, timestamp!!)
    }

    fun broadcastTransaction(rawTransactionHex: String) {
        val apiManager = ApiManager("https://api.blockchair.com")
        val url = "$chainId/push/transaction"

        val body = JsonObject().apply {
            this["data"] = Json.value(rawTransactionHex)
        }.toString()

        apiManager.post(url, body)
    }

    private fun fetchTransactions(
        addresses: List<String>,
        stopHeight: Int? = null,
        receivedAddressItems: List<AddressItem> = emptyList(),
        receivedTransactionItems: List<Transaction> = emptyList()
    ): Pair<List<AddressItem>, List<Transaction>> {
        val params = "?transaction_details=true&limit=$limit,0&offset=${receivedTransactionItems.size},0"
        val url = "$chainId/dashboards/addresses/${addresses.joinToString(separator = ",")}"
        val response = request { apiManager.doOkHttpGet(url + params) }.asObject()
        val data = response.get("data").asObject()

        val addressItems = data.get("addresses").asObject().map {
            val address = it.name
            val script = it.value.asObject().getString("script_hex", "")
            AddressItem(script, address)
        }

        val transactionItems = data.get("transactions").asArray().map {
            val txObject = it.asObject()
            Transaction(
                hash = txObject["hash"].asString(),
                blockId = txObject["block_id"]?.asInt(),
                balanceChange = txObject["balance_change"].asLong(),
                address = txObject["address"].asString()
            )
        }

        val filteredTransactionItems = transactionItems.filter { it.blockId == null || stopHeight == null || it.blockId > stopHeight }
        val addressesMerged = receivedAddressItems + addressItems
        val transactionsMerged = receivedTransactionItems + filteredTransactionItems

        return if (filteredTransactionItems.size < limit) {
            Pair(addressesMerged, transactionsMerged)
        } else {
            fetchTransactions(
                addresses = addresses,
                stopHeight = stopHeight,
                receivedAddressItems = addressesMerged,
                receivedTransactionItems = transactionsMerged
            )
        }
    }

    private fun dateStringToTimestamp(date: String): Long? {
        return try {
            dateFormat.parse(date)?.time?.let { it / 1000 }
        } catch (e: ParseException) {
            null
        }
    }

    private fun fetchBlockHashes(heights: List<Int>): Map<Int, String> {
        val params = "?limit=0"
        val url = "$chainId/dashboards/blocks/${heights.joinToString(separator = ",")}"
        val response = request { apiManager.doOkHttpGet(url + params) }.asObject()

        val map = mutableMapOf<Int, String>()
        val data = response.get("data").asObject()
        data.forEach { blockElement ->
            val block = blockElement.value.asObject()["block"].asObject()
            val blockHeight = block["id"].asInt()
            val blockHash = block["hash"].asString()
            map[blockHeight] = blockHash
        }
        return map
    }

    companion object {
        private const val requestSpacingMs = 250L
        private const val retryBaseDelayMs = 1000L
        private const val maxAttempts = 4

        // Shared across all chains' instances: the endpoint rate-limits per client,
        // not per blockchain.
        private val executor = Executors.newSingleThreadExecutor()
    }

    private data class Transaction(
        val hash: String,
        val blockId: Int?,
        val balanceChange: Long,
        val address: String
    )

}
