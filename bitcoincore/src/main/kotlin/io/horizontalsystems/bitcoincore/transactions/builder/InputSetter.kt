package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.managers.IUnspentOutputSelector
import io.horizontalsystems.bitcoincore.managers.PublicKeyManager
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.storage.InputToSign
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

class InputSetter(
        private val unspentOutputSelector: IUnspentOutputSelector,
        private val publicKeyManager: PublicKeyManager,
        private val addressConverter: IAddressConverter,
        private val changeScriptType: Int,
        private val transactionSizeCalculator: TransactionSizeCalculator
) {
    fun setInputs(mutableTransaction: MutableTransaction, feeRate: Int, senderPay: Boolean) {
        val value = mutableTransaction.recipientValue

        val pluginDataOutputSize = mutableTransaction.getPluginDataOutputSize()
        val unspentOutputInfo = unspentOutputSelector.select(value, feeRate, mutableTransaction.recipientAddress.scriptType, changeScriptType, senderPay, pluginDataOutputSize)

        val unspentOutputs = unspentOutputInfo.outputs
        for (unspentOutput in unspentOutputs) {
            mutableTransaction.addInput(inputToSign(unspentOutput))
        }

        //  calculate fee and add change output if needed
        val fee = unspentOutputInfo.fee

        val receivedValue = if (senderPay) value else value - fee
        mutableTransaction.recipientValue = receivedValue

        if (unspentOutputInfo.addChangeOutput) {
            val changePubKey = publicKeyManager.changePublicKey()
            val changeAddress = addressConverter.convert(changePubKey, changeScriptType)

            val sentValue = if (senderPay) value + fee else value
            val totalValue = unspentOutputs.fold(0L) { sum, unspent -> sum + unspent.output.value }

            mutableTransaction.changeAddress = changeAddress
            mutableTransaction.changeValue = totalValue - sentValue
        }
    }

    fun setInputs(mutableTransaction: MutableTransaction, unspentOutput: UnspentOutput, feeRate: Int) {
        if (unspentOutput.output.scriptType != ScriptType.P2SH) {
            throw TransactionBuilder.BuilderException.NotSupportedScriptType()
        }

        // Calculate fee
        val emptySignature = ByteArray(transactionSizeCalculator.signatureLength)
        val emptyPublicKey = ByteArray(transactionSizeCalculator.pubKeyLength)

        var transactionSize = transactionSizeCalculator.transactionSize(listOf(unspentOutput.output.scriptType), listOf(mutableTransaction.recipientAddress.scriptType), 0)
        unspentOutput.output.signatureScriptFunction?.let { signatureScriptFunction ->
            transactionSize += signatureScriptFunction(listOf(emptySignature, emptyPublicKey)).size
        }

        val fee = transactionSize * feeRate

        if (unspentOutput.output.value < fee) {
            throw TransactionBuilder.BuilderException.FeeMoreThanValue()
        }

        // Add to mutable transaction
        mutableTransaction.addInput(inputToSign(unspentOutput))
        mutableTransaction.recipientValue = unspentOutput.output.value - fee
    }

    private fun inputToSign(unspentOutput: UnspentOutput): InputToSign {
        val previousOutput = unspentOutput.output
        val transactionInput = TransactionInput(previousOutput.transactionHash, previousOutput.index.toLong())

        if (unspentOutput.output.scriptType == ScriptType.P2WPKH) {
            unspentOutput.output.keyHash = unspentOutput.output.keyHash?.drop(2)?.toByteArray()
        }

        val inputToSign = InputToSign(transactionInput, previousOutput, unspentOutput.publicKey)
        return inputToSign
    }
}
