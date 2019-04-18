package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.blocks.IAllPeersSyncedListener

class SendTransactionsOnPeersSynced(var transactionSender: TransactionSender) : IAllPeersSyncedListener {

    override fun onAllPeersSynced() {
        transactionSender.sendPendingTransactions()
    }

}

