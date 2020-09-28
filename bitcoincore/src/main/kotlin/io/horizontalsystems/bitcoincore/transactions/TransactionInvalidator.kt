package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.blocks.IBlockchainDataListener
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.core.ITransactionInfoConverter
import io.horizontalsystems.bitcoincore.models.InvalidTransaction
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.storage.FullTransactionInfo

class TransactionInvalidator(
        private val storage: IStorage,
        private val transactionInfoConverter: ITransactionInfoConverter,
        private val listener: IBlockchainDataListener
) {

    fun invalidate(transaction: Transaction) {
        val invalidTransactionsFullInfo = getDescendantTransactionsFullInfo(transaction.hash)

        if (invalidTransactionsFullInfo.isEmpty()) return

        invalidTransactionsFullInfo.forEach { fullTxInfo ->
            fullTxInfo.header.status = Transaction.Status.INVALID
        }

        val invalidTransactions = invalidTransactionsFullInfo.map { fullTxInfo ->
            val txInfo = transactionInfoConverter.transactionInfo(fullTxInfo)
            val serializedTxInfo = txInfo.serialize()
            InvalidTransaction(fullTxInfo.header, serializedTxInfo, fullTxInfo.rawTransaction)
        }

        storage.moveTransactionToInvalidTransactions(invalidTransactions)
        listener.onTransactionsUpdate(listOf(), invalidTransactions, null)
    }

    private fun getDescendantTransactionsFullInfo(txHash: ByteArray): List<FullTransactionInfo> {
        val fullTransactionInfo = storage.getFullTransactionInfo(txHash) ?: return listOf()
        val list = mutableListOf(fullTransactionInfo)

        val inputs = storage.getTransactionInputsByPrevOutputTxHash(fullTransactionInfo.header.hash)

        inputs.forEach { input ->
            val descendantTxs = getDescendantTransactionsFullInfo(input.transactionHash)
            list.addAll(descendantTxs)
        }

        return list
    }

}
