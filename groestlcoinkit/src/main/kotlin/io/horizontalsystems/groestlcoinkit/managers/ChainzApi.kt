package io.horizontalsystems.groestlcoinkit.managers

import io.horizontalsystems.bitcoincore.core.IInitialSyncApi
import io.horizontalsystems.bitcoincore.managers.ApiManager
import io.horizontalsystems.bitcoincore.managers.TransactionItem
import io.horizontalsystems.bitcoincore.managers.TransactionOutputItem
import java.util.logging.Logger
import kotlin.math.max

class ChainzApi(host: String) : IInitialSyncApi {
    private val apiManager = ApiManager(host)
    private val logger = Logger.getLogger("ChainzApi")
    private val url_prefix = "api.dws?key=d47da926b82e&q="

    override fun getTransactions(addresses: List<String>): List<TransactionItem> {
        logger.info("Request transactions for ${addresses.size} addresses: [${addresses.first()}, ...]")

        val transactions = mutableListOf<TransactionItem>()

        fetchTransactions(addresses, transactions, 0, 50)

        return transactions
    }

    private fun fetchTransactions(addrs: List<String>, txs: MutableList<TransactionItem>, from: Int, to: Int) {
        val joinedAddresses = addrs.joinToString("|")
        val json = apiManager.getJson(url_prefix + "multiaddr&active=$joinedAddresses?from=$from&to=$to")

        val addresses = json["addresses"].asArray()
        val txArray = json["txs"].asArray()
        var oldestTx: String = ""
        var maxconfirms: Int = 0
        for(item in txArray) {
            val tx = item.asObject()
            if(tx["confirmations"].asInt() > maxconfirms)
                oldestTx = tx["hash"].asString()
            maxconfirms = max(maxconfirms, tx["confirmations"].asInt())
        }

        if(oldestTx != "") {
            val txinfoJson = apiManager.getJson(url_prefix + "txinfo&t=$oldestTx")

            val blockheight = txinfoJson["block"].asInt()

            var blockHash: String = apiManager.getString(url_prefix + "getblockhash&height=$blockheight")
            blockHash = blockHash.subSequence(1, 64).toString() // remove the \" at the beginning and end

            val outputs = mutableListOf<TransactionOutputItem>()


            for (outputItem in txinfoJson["outputs"].asArray()) {
                val outputJson = outputItem.asObject()

                val script = (outputJson["script"] ?: continue).asString()
                val address = (outputJson["addr"] ?: continue)

                outputs.add(TransactionOutputItem(script, address.asString()))
            }

            txs.add(TransactionItem(blockHash, blockheight, outputs))

        }
    }
}