package io.horizontalsystems.bitcoincore.apisync

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import io.horizontalsystems.bitcoincore.apisync.model.AddressItem
import io.horizontalsystems.bitcoincore.apisync.model.TransactionItem
import io.horizontalsystems.bitcoincore.core.IApiTransactionProvider
import io.horizontalsystems.bitcoincore.managers.ApiManager
import java.util.logging.Logger

class BCoinApi(host: String) : IApiTransactionProvider {
    private val apiManager = ApiManager(host)
    private val logger = Logger.getLogger("BCoinApi")

    override fun transactions(addresses: List<String>, stopHeight: Int?): List<TransactionItem> {
        val requestData = JsonObject().apply {
            this["addresses"] = Json.array(*addresses.toTypedArray())
        }

        logger.info("Request transactions for ${addresses.size} addresses: [${addresses.first()}, ...]")

        val response = apiManager.post("tx/address", requestData.toString()).asArray()

        logger.info("Got ${response.size()} transactions for requested addresses")

        val transactions = mutableListOf<TransactionItem>()

        for (txItem in response) {
            val tx = txItem.asObject()

            val blockHashJson = tx["block"] ?: continue
            val blockHash = if (blockHashJson.isString) blockHashJson.asString() else continue

            val outputs = mutableListOf<AddressItem>()
            for (outputItem in tx["outputs"].asArray()) {
                val outputJson = outputItem.asObject()

                val scriptJson = outputJson["script"] ?: continue
                val addressJson = outputJson["address"]

                if (scriptJson.isString) {
                    outputs.add(AddressItem(scriptJson.asString(), addressJson?.asString()))
                }
            }

            transactions.add(TransactionItem(blockHash, tx["height"].asInt(), outputs))
        }

        return transactions
    }

}
