package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.network.peer.PeerGroup
import io.horizontalsystems.bitcoinkit.network.peer.task.SendTransactionTask

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