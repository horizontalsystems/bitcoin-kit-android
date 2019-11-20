package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.network.peer.IPeerTaskHandler
import io.horizontalsystems.bitcoincore.network.peer.Peer
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup
import io.horizontalsystems.bitcoincore.network.peer.task.PeerTask
import io.horizontalsystems.bitcoincore.network.peer.task.SendTransactionTask

class TransactionSender(
        private val transactionSyncer: TransactionSyncer,
        private val peerGroup: PeerGroup
) : IPeerTaskHandler {

    fun sendPendingTransactions() {
        try {
            peerGroup.checkPeersSynced()

            peerGroup.someReadyPeers().forEach { peer ->
                transactionSyncer.getPendingTransactions().forEach { pendingTransaction ->
                    peer.addTask(SendTransactionTask(pendingTransaction))
                }
            }


        } catch (e: PeerGroup.Error) {
//            logger.warning("Handling pending transactions failed with: ${e.message}")
        }

    }

    fun canSendTransaction() {
        peerGroup.checkPeersSynced()
    }

    // IPeerTaskHandler

    override fun handleCompletedTask(peer: Peer, task: PeerTask): Boolean {
        return when (task) {
            is SendTransactionTask -> {
                transactionSyncer.handleTransaction(task.transaction)
                true
            }
            else -> false
        }
    }

}