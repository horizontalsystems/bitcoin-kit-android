package io.horizontalsystems.dashkit

import android.util.Log
import io.horizontalsystems.bitcoincore.core.toHexString
import io.horizontalsystems.dashkit.tasks.RequestTransactionLockRequestsTask
import io.horizontalsystems.dashkit.tasks.RequestTransactionLockVotesTask
import io.horizontalsystems.bitcoincore.models.InventoryItem
import io.horizontalsystems.bitcoincore.network.peer.IInventoryItemsHandler
import io.horizontalsystems.bitcoincore.network.peer.IPeerTaskHandler
import io.horizontalsystems.bitcoincore.network.peer.Peer
import io.horizontalsystems.bitcoincore.network.peer.task.PeerTask
import io.horizontalsystems.bitcoincore.transactions.TransactionSyncer

class InstantSend(private val transactionSyncer: TransactionSyncer?) : IInventoryItemsHandler, IPeerTaskHandler {

    override fun handleInventoryItems(peer: Peer, inventoryItems: List<InventoryItem>) {
        val transactionLockRequests = mutableListOf<ByteArray>()
        val transactionLockVotes = mutableListOf<ByteArray>()

        inventoryItems.forEach { item ->
            when (item.type) {
                InventoryType.MSG_TXLOCK_REQUEST -> {
                    transactionLockRequests.add(item.hash)
                }
                InventoryType.MSG_TXLOCK_VOTE -> {
                    transactionLockVotes.add(item.hash)
                }
            }
        }

        if (transactionLockRequests.isNotEmpty()) {
            peer.addTask(RequestTransactionLockRequestsTask(transactionLockRequests))
        }

        if (transactionLockVotes.isNotEmpty()) {
            peer.addTask(RequestTransactionLockVotesTask(transactionLockVotes))
        }

    }

    override fun handleCompletedTask(peer: Peer, task: PeerTask): Boolean {
        return when (task) {
            is RequestTransactionLockRequestsTask -> {
                transactionSyncer?.handleTransactions(task.transactions)
                true
            }
            is RequestTransactionLockVotesTask -> {
                task.transactionLockVotes.forEach {
                    Log.e("AAA", "Received tx lock vote for tx: ${it.txHash.reversedArray().toHexString()}")
                }
                true
            }
            else -> false
        }
    }

}