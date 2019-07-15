package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.managers.BloomFilterManager
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.builder.TransactionBuilder

class TransactionCreator(private val builder: TransactionBuilder,
                         private val processor: TransactionProcessor,
                         private val transactionSender: TransactionSender,
                         private val bloomFilterManager: BloomFilterManager) {

    @Throws
    fun create(address: String, value: Long, feeRate: Int, senderPay: Boolean): FullTransaction {
        transactionSender.canSendTransaction()

        val transaction = builder.buildTransaction(value, address, feeRate, senderPay)

        try {
            processor.processOutgoing(transaction)
        } catch (ex: BloomFilterManager.BloomFilterExpired) {
            bloomFilterManager.regenerateBloomFilter()
        }

        transactionSender.sendPendingTransactions()

        return transaction
    }

    @Throws
    fun create(unspentOutput: UnspentOutput, address: String, feeRate: Int, signatureScriptFunction: (ByteArray, ByteArray) -> ByteArray): FullTransaction {
        transactionSender.canSendTransaction()

        val transaction = builder.buildTransaction(unspentOutput, address, feeRate, signatureScriptFunction)

        try {
            processor.processOutgoing(transaction)
        } catch (ex: BloomFilterManager.BloomFilterExpired) {
            bloomFilterManager.regenerateBloomFilter()
        }

        transactionSender.sendPendingTransactions()

        return transaction
    }

    open class TransactionCreationException(msg: String) : Exception(msg)
    class TransactionAlreadyExists(msg: String) : TransactionCreationException(msg)

}
