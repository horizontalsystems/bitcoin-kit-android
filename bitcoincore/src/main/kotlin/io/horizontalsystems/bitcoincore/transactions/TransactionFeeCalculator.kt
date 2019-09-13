package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.managers.IUnspentOutputSelector
import io.horizontalsystems.bitcoincore.managers.SelectedUnspentOutputInfo
import io.horizontalsystems.bitcoincore.models.Address

class TransactionFeeCalculator(private val unspentOutputSelector: IUnspentOutputSelector, private val transactionSizeCalculator: TransactionSizeCalculator) {

    fun fee(value: Long, feeRate: Int, senderPay: Boolean, toAddress: Address?, changeAddress: Address): Long {
        val outputScriptType = toAddress?.scriptType ?: changeAddress.scriptType

        return unspentOutputSelector.select(
                value = value,
                feeRate = feeRate,
                outputType = outputScriptType,
                changeType = changeAddress.scriptType,
                senderPay = senderPay
        ).fee
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
