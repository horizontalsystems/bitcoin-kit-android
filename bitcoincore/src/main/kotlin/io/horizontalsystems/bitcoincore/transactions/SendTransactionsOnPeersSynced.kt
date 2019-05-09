package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.blocks.IPeerSyncListener

class SendTransactionsOnPeersSynced(var transactionSender: TransactionSender) : IPeerSyncListener {

    override fun onAllPeersSynced() {
        transactionSender.sendPendingTransactions()
    }

}

