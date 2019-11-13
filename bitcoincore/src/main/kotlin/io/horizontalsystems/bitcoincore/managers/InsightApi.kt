package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.IInitialSyncApi
import io.horizontalsystems.bitcoincore.core.getOrMappingError
import java.util.logging.Logger

class InsightApi(host: String) : IInitialSyncApi {
    private val apiManager = ApiManager(host)
    private val logger = Logger.getLogger("InsightApi")

    override fun getTransactions(addresses: List<String>): List<TransactionItem> {
        logger.info("Request transactions for ${addresses.size} addresses: [${addresses.first()}, ...]")

        val transactions = mutableListOf<TransactionItem>()

        fetchTransactions(addresses, transactions, 0, 50)

        return transactions
    }

    private fun fetchTransactions(addrs: List<String>, txs: MutableList<TransactionItem>, from: Int, to: Int) {
        val joinedAddresses = addrs.joinToString(",")
        val json = apiManager.getJson("addrs/$joinedAddresses/txs?from=$from&to=$to")

        val totalItems = json.getOrMappingError("totalItems").asInt()
        val receivedTo = json.getOrMappingError("to").asInt()

        val items = json.getOrMappingError("items").asArray()
        for (item in items) {
            val tx = item.asObject()

            val blockHash = tx.getOrMappingError("blockhash")
            val blockheight = tx.getOrMappingError("blockheight")
            val outputs = mutableListOf<TransactionOutputItem>()

            for (outputItem in tx.getOrMappingError("vout").asArray()) {
                val outputJson = outputItem.asObject()

                val scriptJson = outputJson.getOrMappingError("scriptPubKey").asObject()
                val script = scriptJson.getOrMappingError("hex").asString()
                val addresses = scriptJson.getOrMappingError("addresses").asArray()

                outputs.add(TransactionOutputItem(script, addresses[0].asString()))
            }

            txs.add(TransactionItem(blockHash.asString(), blockheight.asInt(), outputs))
        }

        if (totalItems > to) {
            fetchTransactions(addrs, txs, receivedTo, receivedTo + 50)
        }
    }
}
