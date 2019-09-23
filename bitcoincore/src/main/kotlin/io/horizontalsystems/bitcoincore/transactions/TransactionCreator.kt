package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.managers.BloomFilterManager
import io.horizontalsystems.bitcoincore.managers.PublicKeyManager
import io.horizontalsystems.bitcoincore.models.Address
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
        private val transactionFeeCalculator: TransactionFeeCalculator,
        private val storage: IStorage,
        private val bip: Bip) {

    @Throws
    fun create(toAddress: String, value: Long, feeRate: Int, senderPay: Boolean): FullTransaction {
        transactionSender.canSendTransaction()

        val address = addressConverter.convert(toAddress)
        val feeWithUnspentOutputs = transactionFeeCalculator.feeWithUnspentOutputs(value, feeRate, address.scriptType, bip.scriptType, senderPay)
        var changeAddress: Address? = null
        if (feeWithUnspentOutputs.addChangeOutput) {
            val changePubKey = publicKeyManager.changePublicKey()
            changeAddress = addressConverter.convert(changePubKey, bip.scriptType)
        }

        val lastBlockHeight = storage.lastBlock()?.height ?: 0
        val transaction = builder.buildTransaction(
                value,
                feeWithUnspentOutputs.outputs,
                feeWithUnspentOutputs.fee,
                senderPay,
                address,
                changeAddress,
                lastBlockHeight.toLong()
        )

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
        val fee = transactionFeeCalculator.fee(unspentOutput.output.scriptType, address.scriptType, feeRate, signatureScriptFunction)
        val lastBlockHeight = storage.lastBlock()?.height ?: 0

        val transaction = builder.buildTransaction(unspentOutput, address, fee, lastBlockHeight.toLong(), signatureScriptFunction)

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
