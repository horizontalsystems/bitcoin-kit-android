package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.blocks.IAllPeersSyncedListener

class SendTransactionsOnPeersSynced(var transactionSender: TransactionSender) : IAllPeersSyncedListener {

    override fun onAllPeersSynced() {
        transactionSender.sendPendingTransactions()
    }

}

