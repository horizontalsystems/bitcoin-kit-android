package io.horizontalsystems.bitcoincore.managers

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import io.horizontalsystems.bitcoincore.core.IInitialSyncApi
import io.horizontalsystems.bitcoincore.extensions.toReversedByteArray
import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.utils.HSLogger
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

class BlockHashFetcher(private val addressSelector: IAddressSelector, private val addressConverter: IAddressConverter, private val initialSyncerApi: IInitialSyncApi, private val helper: BlockHashFetcherHelper) {

    fun getBlockHashes(publicKeys: List<PublicKey>): Pair<List<BlockHash>, Int> {
        val addresses = publicKeys.map {
            addressSelector.getAddressVariants(addressConverter, it)
        }

        val transactions = initialSyncerApi.getTransactions(addresses.flatten())

        if (transactions.isEmpty()) {
            return Pair(listOf(), -1)
        }

        val lastUsedIndex = helper.lastUsedIndex(addresses, transactions.map { it.txOutputs }.flatten())

        val blockHashes = transactions.map {
            BlockHash(it.blockHash.toReversedByteArray(), it.blockHeight, 0)
        }

        return Pair(blockHashes, lastUsedIndex)
    }

}

class BlockHashFetcherHelper {

    fun lastUsedIndex(addresses: List<List<String>>, outputs: List<TransactionOutputItem>): Int {
        val searchAddressStrings = outputs.map { it.address }
        val searchScriptStrings = outputs.map { it.script }

        for (i in addresses.size - 1 downTo 0) {
            addresses[i].forEach { address ->
                if (searchAddressStrings.contains(address) || searchScriptStrings.any { script -> script.contains(address) }) {
                    return i
                }
            }
        }

        return -1
    }

}

class BCoinApi(private val host: String) : IInitialSyncApi {
    private val httpRequester = HttpRequester()
    private val logger = HSLogger("BCoinApi")

    override fun getTransactions(addresses: List<String>): List<TransactionItem> {
        val requestData = JsonObject().apply {
            this["addresses"] = Json.array(*addresses.toTypedArray())
        }

        logger.i("Request transactions for ${addresses.size} addresses: [${addresses.first()}, ...]")

        val response = httpRequester.post("$host/tx/address", requestData.toString()).asArray()

        logger.i("Got ${response.size()} transactions for requested addresses")

        val transactions = mutableListOf<TransactionItem>()

        for (txItem in response) {
            val tx = txItem.asObject()

            val blockHashJson = tx["block"] ?: continue
            val blockHash = if (blockHashJson.isString) blockHashJson.asString() else continue

            val outputs = mutableListOf<TransactionOutputItem>()
            for (outputItem in tx["outputs"].asArray()) {
                val outputJson = outputItem.asObject()

                val scriptJson = outputJson["script"] ?: continue
                val addressJson = outputJson["address"] ?: continue

                if (scriptJson.isString && addressJson.isString) {
                    outputs.add(TransactionOutputItem(scriptJson.asString(), addressJson.asString()))
                }
            }

            transactions.add(TransactionItem(blockHash, tx["height"].asInt(), outputs))
        }

        return transactions
    }

}

data class TransactionItem(val blockHash: String, val blockHeight: Int, val txOutputs: List<TransactionOutputItem>)
data class TransactionOutputItem(val script: String, val address: String)
