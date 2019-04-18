package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.network.peer.PeerGroup
import io.horizontalsystems.bitcoincore.network.peer.task.SendTransactionTask

class TransactionSender {
    var transactionSyncer: TransactionSyncer? = null
    var peerGroup: PeerGroup? = null

    fun sendPendingTransactions() {
        try {
            peerGroup?.checkPeersSynced()

            peerGroup?.someReadyPeers()?.forEach { peer ->
                transactionSyncer?.getPendingTransactions()?.forEach { pendingTransaction ->
                    peer.addTask(SendTransactionTask(pendingTransaction))
                }
            }


        } catch (e: PeerGroup.Error) {
//            logger.warning("Handling pending transactions failed with: ${e.message}")
        }

    }

    fun canSendTransaction() {
        peerGroup?.checkPeersSynced()
    }

}