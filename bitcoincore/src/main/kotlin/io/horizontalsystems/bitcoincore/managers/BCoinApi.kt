package io.horizontalsystems.bitcoincore.managers

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import io.horizontalsystems.bitcoincore.core.IInitialSyncApi
import io.horizontalsystems.bitcoincore.core.getOrMappingError
import java.util.logging.Logger

class BCoinApi(private val host: String) : IInitialSyncApi {
    private val apiManager = ApiManager(host)
    private val logger = Logger.getLogger("BCoinApi")

    override fun getTransactions(addresses: List<String>): List<TransactionItem> {
        val requestData = JsonObject().apply {
            this["addresses"] = Json.array(*addresses.toTypedArray())
        }

        logger.info("Request transactions for ${addresses.size} addresses: [${addresses.first()}, ...]")

        val response = apiManager.post("tx/address", requestData.toString()).asArray()

        logger.info("Got ${response.size()} transactions for requested addresses")

        val transactions = mutableListOf<TransactionItem>()

        for (txItem in response) {
            val tx = txItem.asObject()

            val blockHash = tx.getOrMappingError("block").asString()

            val outputs = mutableListOf<TransactionOutputItem>()
            for (outputItem in tx.getOrMappingError("outputs").asArray()) {
                val outputJson = outputItem.asObject()

                val script = outputJson.getOrMappingError("script").asString()
                val address = outputJson.getOrMappingError("address").asString()

                outputs.add(TransactionOutputItem(script, address))
            }

            transactions.add(TransactionItem(blockHash, tx.getOrMappingError("height").asInt(), outputs))
        }

        return transactions
    }

}
