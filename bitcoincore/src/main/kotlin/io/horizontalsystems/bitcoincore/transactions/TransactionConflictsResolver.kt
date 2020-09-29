package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.storage.FullTransaction

class TransactionConflictsResolver(private val storage: IStorage) {

    // Only pending transactions may be conflicting with a transaction in block. No need to check that\
    fun getTransactionsConflictingWithInBlockTransaction(transaction: FullTransaction): List<Transaction> {
        return getConflictingTransactionsForTransaction(transaction)
    }

    fun getTransactionsConflictingWithPendingTransaction(transaction: FullTransaction): List<Transaction> {
        val conflictingTransactions = getConflictingTransactionsForTransaction(transaction)

        if (conflictingTransactions.isEmpty()) return listOf()

        // If any of conflicting transactions is already in a block, then current transaction is invalid and non of them is conflicting with it.
        if (conflictingTransactions.any { it.blockHash != null }) return listOf()

        return conflictingTransactions
    }

    fun getIncomingPendingTransactionsConflictingWith(transaction: FullTransaction): List<Transaction> {
        val incomingPendingTxHashes = storage.getIncomingPendingTxHashes()

        if (incomingPendingTxHashes.isEmpty()) return listOf()

        val conflictingTransactionHashes = storage
                .getTransactionInputs(incomingPendingTxHashes)
                .filter { input ->
                    transaction.inputs.any { it.previousOutputIndex == input.previousOutputIndex && it.previousOutputTxHash.contentEquals(input.previousOutputTxHash) }
                }
                .map {
                    it.transactionHash
                }

        if (conflictingTransactionHashes.isEmpty()) return listOf()

        return conflictingTransactionHashes.mapNotNull {
            storage.getTransaction(it)
        }.filter {
            it.blockHash == null
        }
    }

    private fun getConflictingTransactionsForTransaction(transaction: FullTransaction): List<Transaction> {
        return transaction.inputs.mapNotNull { input ->
            val conflictingTxHash = storage.getTransactionInput(input.previousOutputTxHash, input.previousOutputIndex)?.transactionHash
            when {
                conflictingTxHash == null -> null
                conflictingTxHash.contentEquals(transaction.header.hash) -> null
                else -> conflictingTxHash
            }
        }.mapNotNull {
            storage.getTransaction(it)
        }
    }
}
