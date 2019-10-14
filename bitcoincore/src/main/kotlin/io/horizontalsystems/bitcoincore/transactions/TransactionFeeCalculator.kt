package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.managers.PublicKeyManager
import io.horizontalsystems.bitcoincore.transactions.builder.InputSetter
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.builder.OutputSetter
import io.horizontalsystems.bitcoincore.utils.AddressConverterChain

class TransactionFeeCalculator(
        private val transactionSizeCalculator: TransactionSizeCalculator,
        private val outputSetter: OutputSetter,
        private val inputSetter: InputSetter,
        private val addressConverter: AddressConverterChain,
        private val publicKeyManager: PublicKeyManager,
        private val changeScriptType: Int
) {

    fun fee(value: Long, feeRate: Int, senderPay: Boolean, toAddress: String?, pluginData: Map<String, Map<String, Any>>): Long {
        val mutableTransaction = MutableTransaction()

        outputSetter.setOutputs(mutableTransaction, toAddress ?: sampleAddress(), value, pluginData)
        inputSetter.setInputs(mutableTransaction, feeRate, senderPay)

        val inputsTotalValue = mutableTransaction.inputsToSign.map { it.previousOutput.value }.sum()
        val outputsTotalValue = mutableTransaction.recipientValue + mutableTransaction.changeValue

        return inputsTotalValue - outputsTotalValue
    }

    fun fee(inputScriptType: Int, outputScriptType: Int, feeRate: Int, signatureScriptFunction: (ByteArray, ByteArray) -> ByteArray): Long {
        val emptySignature = ByteArray(transactionSizeCalculator.signatureLength)
        val emptyPublicKey = ByteArray(transactionSizeCalculator.pubKeyLength)

        val transactionSize = transactionSizeCalculator.transactionSize(listOf(inputScriptType), listOf(outputScriptType), 0) +
                signatureScriptFunction(emptySignature, emptyPublicKey).size

        return transactionSize * feeRate
    }

    private fun sampleAddress(): String {
        return addressConverter.convert(publicKey = publicKeyManager.changePublicKey(), scriptType = changeScriptType).string
    }
}
