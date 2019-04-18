package io.horizontalsystems.bitcoinkit.dash.tasks

import io.horizontalsystems.bitcoinkit.dash.InventoryType
import io.horizontalsystems.bitcoinkit.dash.messages.TransactionLockMessage
import io.horizontalsystems.bitcoincore.models.InventoryItem
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.peer.task.PeerTask
import io.horizontalsystems.bitcoincore.storage.FullTransaction

class RequestTransactionLockRequestsTask(hashes: List<ByteArray>) : PeerTask() {

    val hashes = hashes.toMutableList()
    var transactions = mutableListOf<FullTransaction>()

    override fun start() {
        requester?.getData(hashes.map { hash ->
            InventoryItem(InventoryType.MSG_TXLOCK_REQUEST, hash)
        })
    }

    override fun handleMessage(message: IMessage) = when (message) {
        is TransactionLockMessage -> handleTransactionLockRequest(message.transaction)
        else -> false
    }

    private fun handleTransactionLockRequest(transaction: FullTransaction): Boolean {
        val hash = hashes.firstOrNull { it.contentEquals(transaction.header.hash) } ?: return false

        hashes.remove(hash)
        transactions.add(transaction)

        if (hashes.isEmpty()) {
            listener?.onTaskCompleted(this)
        }

        return true
    }

}
