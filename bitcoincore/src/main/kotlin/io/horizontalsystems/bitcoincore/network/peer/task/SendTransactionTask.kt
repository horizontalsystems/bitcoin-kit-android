package io.horizontalsystems.bitcoincore.network.peer.task

import io.horizontalsystems.bitcoincore.models.InventoryItem
import io.horizontalsystems.bitcoincore.network.messages.GetDataMessage
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.messages.InvMessage
import io.horizontalsystems.bitcoincore.network.messages.TransactionMessage
import io.horizontalsystems.bitcoincore.storage.FullTransaction

class SendTransactionTask(val transaction: FullTransaction) : PeerTask() {
    override fun start() {
        requester?.send(InvMessage(InventoryItem.MSG_TX, transaction.header.hash))
    }

    override fun handleMessage(message: IMessage): Boolean {
        if (message !is GetDataMessage) {
            return false
        }

        for (inv in message.inventory) {
            if (handleGetDataInventoryItem(inv)) {
                continue
            }
        }

        return true
    }

    private fun handleGetDataInventoryItem(item: InventoryItem): Boolean {
        if (item.type == InventoryItem.MSG_TX && item.hash.contentEquals(transaction.header.hash)) {
            requester?.send(TransactionMessage(transaction, 0))
            listener?.onTaskCompleted(this)

            return true
        }

        return false
    }
}
