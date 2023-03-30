package io.horizontalsystems.ecash

import chronik.Chronik
import io.horizontalsystems.bitcoincore.core.IInitialSyncApi
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.managers.ApiManager
import io.horizontalsystems.bitcoincore.managers.TransactionItem
import io.horizontalsystems.bitcoincore.managers.TransactionOutputItem

class ChronikApi : IInitialSyncApi {
    private val apiManager = ApiManager("https://chronik.fabien.cash")

    override fun getTransactions(addresses: List<String>): List<TransactionItem> {
        val transactionItems = mutableListOf<TransactionItem>()

        for (address in addresses) {
            try {
                val get = apiManager.get("script/p2pkh/$address/history")
                val txHistoryPage = Chronik.TxHistoryPage.parseFrom(get)
                transactionItems.addAll(
                    txHistoryPage.txsList.map {
                        TransactionItem(
                            blockHash = it.block.hash.toByteArray().toReversedHex(),
                            blockHeight = it.block.height,
                            txOutputs = it.outputsList.map { txOutput ->
                                TransactionOutputItem(
                                    script = txOutput.outputScript.toByteArray().toReversedHex(),
                                    address = ""
                                )
                            }
                        )
                    }
                )
            } catch (e: Throwable) {
                continue
            }
            Thread.sleep(10)
        }

        return transactionItems
    }
}
