package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoincore.managers.BloomFilterManager
import io.horizontalsystems.bitcoincore.managers.PublicKeyManager
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.builder.TransactionBuilder
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

class TransactionCreator(
        private val builder: TransactionBuilder,
        private val processor: TransactionProcessor,
        private val transactionSender: TransactionSender,
        private val bloomFilterManager: BloomFilterManager,
        private val publicKeyManager: PublicKeyManager,
        private val addressConverter: IAddressConverter,
        private val bip: Bip) {

    @Throws
    fun fee(value: Long, toAddress: String?, senderPay: Boolean = true, feeRate: Int): Long {
        val address = if (toAddress == null) null else {
            addressConverter.convert(toAddress)
        }

        val changePublicKey = publicKeyManager.changePublicKey()
        val changeAddress = addressConverter.convert(changePublicKey, bip.scriptType)

        return builder.fee(value, feeRate, senderPay, address, changeAddress)
    }

    @Throws
    fun create(toAddress: String, value: Long, feeRate: Int, senderPay: Boolean): FullTransaction {
        transactionSender.canSendTransaction()

        val address = addressConverter.convert(toAddress)
        val changePublicKey = publicKeyManager.changePublicKey()
        val changeAddress = addressConverter.convert(changePublicKey, bip.scriptType)

        val transaction = builder.buildTransaction(value, address, feeRate, senderPay, changeAddress)

        try {
            processor.processOutgoing(transaction)
        } catch (ex: BloomFilterManager.BloomFilterExpired) {
            bloomFilterManager.regenerateBloomFilter()
        }

        transactionSender.sendPendingTransactions()

        return transaction
    }

    @Throws
    fun create(unspentOutput: UnspentOutput, toAddress: String, feeRate: Int, signatureScriptFunction: (ByteArray, ByteArray) -> ByteArray): FullTransaction {
        transactionSender.canSendTransaction()

        val address = addressConverter.convert(toAddress)
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
