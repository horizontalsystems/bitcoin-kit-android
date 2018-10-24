package io.horizontalsystems.bitcoinkit.network.PeerTask

import io.horizontalsystems.bitcoinkit.models.InventoryItem
import io.horizontalsystems.bitcoinkit.models.Transaction

class RequestTransactionsTask(hashes: List<ByteArray>) : PeerTask() {
    val hashes = hashes.toMutableList()
    var transactions = mutableListOf<Transaction>()

    override fun start() {
        requester?.getData(hashes.map { hash ->
            InventoryItem(InventoryItem.MSG_TX, hash)
        })
    }

    override fun handleTransaction(transaction: Transaction): Boolean {
        val hash = hashes.firstOrNull { it.contentEquals(transaction.hash) } ?: return false

        hashes.remove(hash)
        transactions.add(transaction)

        if (hashes.isEmpty()) {
            delegate?.onTaskCompleted(this)
        }

        return true
    }

    override fun isRequestingInventory(hash: ByteArray): Boolean {
        return hashes.any { it.contentEquals(hash) }
    }

}
