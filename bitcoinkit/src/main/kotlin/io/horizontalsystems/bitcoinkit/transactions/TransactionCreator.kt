package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.transactions.builder.TransactionBuilder

class TransactionCreator(private val builder: TransactionBuilder, private val processor: TransactionProcessor, private val transactionSender: TransactionSender) {

    @Throws
    fun create(address: String, value: Long, feeRate: Int, senderPay: Boolean) {
        transactionSender.canSendTransaction()

        val transaction = builder.buildTransaction(value, address, feeRate, senderPay)

        processor.processOutgoing(transaction)

        transactionSender.sendPendingTransactions()
    }

    open class TransactionCreationException(msg: String) : Exception(msg)
    class TransactionAlreadyExists(msg: String) : TransactionCreationException(msg)

}
