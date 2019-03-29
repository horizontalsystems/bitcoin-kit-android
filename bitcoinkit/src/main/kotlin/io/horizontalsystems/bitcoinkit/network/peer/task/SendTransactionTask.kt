package io.horizontalsystems.bitcoinkit.network.peer.task

import io.horizontalsystems.bitcoinkit.models.InventoryItem
import io.horizontalsystems.bitcoinkit.storage.FullTransaction

class SendTransactionTask(val transaction: FullTransaction) : PeerTask() {
    override fun start() {
        requester?.sendTransactionInventory(transaction.header.hash)
    }

    override fun handleGetDataInventoryItem(item: InventoryItem): Boolean {
        if (item.type == InventoryItem.MSG_TX && item.hash.contentEquals(transaction.header.hash)) {
            requester?.send(transaction)
            listener?.onTaskCompleted(this)

            return true
        }

        return false
    }
}
