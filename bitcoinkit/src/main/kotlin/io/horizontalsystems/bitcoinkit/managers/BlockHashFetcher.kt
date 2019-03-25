package io.horizontalsystems.bitcoinkit.managers

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import io.horizontalsystems.bitcoinkit.extensions.toReversedByteArray
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.network.*
import java.util.logging.Logger

class BlockHashFetcher(private val addressSelector: IAddressSelector, private val bCoinApi: BCoinApi, private val helper: BlockHashFetcherHelper) {

    fun getBlockHashes(publicKeys: List<PublicKey>): Pair<List<BlockHash>, Int> {
        val addresses = publicKeys.map {
            addressSelector.getAddressVariants(it)
        }

        val transactions = bCoinApi.getTransactions(addresses.flatten())

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

    fun lastUsedIndex(addresses: List<List<String>>, outputs: List<BCoinApi.TransactionOutputItem>): Int {
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

class BCoinApi(val network: Network, val httpRequester: HttpRequester) {

    val host = when (network) {
        is MainNet -> "https://btc.horizontalsystems.xyz/apg"
        is TestNet -> "http://btc-testnet.horizontalsystems.xyz/apg"
        is MainNetBitcoinCash -> "https://bch.horizontalsystems.xyz/apg"
        is TestNetBitcoinCash -> "http://bch-testnet.horizontalsystems.xyz/apg"
        else -> "http://btc-testnet.horizontalsystems.xyz/apg"
    }

    private val logger = Logger.getLogger("BCoinApi")

    fun getTransactions(addresses: List<String>): List<TransactionItem> {
        val requestData = JsonObject().apply {
            this["addresses"] = Json.array(*addresses.toTypedArray())
        }

        logger.info("Request transactions for ${addresses.size} addresses: [${addresses.first()}, ...]")

        val response = httpRequester.post("$host/tx/address", requestData.toString()).asArray()

        logger.info("Got ${response.size()} transactions for requested addresses")

        val transactions = mutableListOf<TransactionItem>()

        for (txItem in response) {
            val tx = txItem.asObject()

            val outputs = mutableListOf<TransactionOutputItem>()
            for (outputItem in tx["outputs"].asArray()) {
                val outputJson = outputItem.asObject()

                val scriptJson = outputJson["script"] ?: continue
                val addressJson = outputJson["address"] ?: continue

                outputs.add(TransactionOutputItem(scriptJson.asString(), addressJson.asString()))
            }

            transactions.add(TransactionItem(tx["block"].asString(), tx["height"].asInt(), outputs))
        }

        return transactions
    }

    data class TransactionItem(val blockHash: String, val blockHeight: Int, val txOutputs: List<TransactionOutputItem>)
    data class TransactionOutputItem(val script: String, val address: String)

}

