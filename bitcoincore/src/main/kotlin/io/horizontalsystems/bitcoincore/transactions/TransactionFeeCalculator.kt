package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.managers.IUnspentOutputSelector
import io.horizontalsystems.bitcoincore.managers.SelectedUnspentOutputInfo
import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.serializers.TransactionSerializer
import io.horizontalsystems.bitcoincore.transactions.builder.TransactionBuilder

class TransactionFeeCalculator(
        private val unspentOutputSelector: IUnspentOutputSelector,
        private val transactionSizeCalculator: TransactionSizeCalculator,
        private val transactionBuilder: TransactionBuilder) {

    fun fee(value: Long, feeRate: Int, senderPay: Boolean, toAddress: Address?, changeAddress: Address): Long {
        if (toAddress == null) {
            return unspentOutputSelector.select(
                    value = value,
                    feeRate = feeRate,
                    outputType = changeAddress.scriptType,
                    changeType = changeAddress.scriptType,
                    senderPay = senderPay
            ).fee
        }

        val selectedOutputsInfo = unspentOutputSelector.select(
                value = value,
                feeRate = feeRate,
                outputType = toAddress.scriptType,
                changeType = changeAddress.scriptType,
                senderPay = senderPay
        )

        val toChangeAddress = if (selectedOutputsInfo.addChangeOutput) changeAddress else null
        val transaction = transactionBuilder.buildTransaction(
                value,
                selectedOutputsInfo.outputs,
                selectedOutputsInfo.fee,
                senderPay,
                toAddress,
                toChangeAddress)

        return TransactionSerializer.serialize(transaction, withWitness = false).size * feeRate.toLong()
    }

    fun fee(inputScriptType: Int, outputScriptType: Int, feeRate: Int, signatureScriptFunction: (ByteArray, ByteArray) -> ByteArray): Long {
        val emptySignature = ByteArray(transactionSizeCalculator.signatureLength)
        val emptyPublicKey = ByteArray(transactionSizeCalculator.pubKeyLength)

        val transactionSize = transactionSizeCalculator.transactionSize(listOf(inputScriptType), listOf(outputScriptType)) +
                signatureScriptFunction(emptySignature, emptyPublicKey).size

        return transactionSize * feeRate
    }

    fun feeWithUnspentOutputs(value: Long, feeRate: Int, toScriptType: Int, changeScriptType: Int, senderPay: Boolean): SelectedUnspentOutputInfo {
        return unspentOutputSelector.select(value, feeRate, toScriptType, changeScriptType, senderPay)
    }
}
