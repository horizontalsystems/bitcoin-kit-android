package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.storage.FullTransaction

class TransactionConflictsResolver {
    fun getIncomingPendingTransactionsConflictingWith(fullTransaction: FullTransaction): List<Transaction> {
        TODO("Not yet implemented")
    }

    fun getTransactionsConflictingWithInBlockTransaction(fullTransaction: FullTransaction): List<Transaction> {
        TODO("Not yet implemented")
    }

    fun getTransactionsConflictingWithPendingTransaction(transaction: FullTransaction): List<Transaction> {
        TODO("Not yet implemented")
    }
}
