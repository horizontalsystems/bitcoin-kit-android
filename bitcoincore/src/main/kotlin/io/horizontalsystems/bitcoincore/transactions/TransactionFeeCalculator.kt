package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.managers.PublicKeyManager
import io.horizontalsystems.bitcoincore.transactions.builder.InputSetter
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.builder.OutputSetter
import io.horizontalsystems.bitcoincore.utils.AddressConverterChain

class TransactionFeeCalculator(
        private val outputSetter: OutputSetter,
        private val inputSetter: InputSetter,
        private val addressConverter: AddressConverterChain,
        private val publicKeyManager: PublicKeyManager,
        private val changeScriptType: Int
) {

    fun fee(value: Long, feeRate: Int, senderPay: Boolean, toAddress: String?, pluginData: Map<Byte, Map<String, Any>>): Long {
        val mutableTransaction = MutableTransaction()

        outputSetter.setOutputs(mutableTransaction, toAddress ?: sampleAddress(), value, pluginData)
        inputSetter.setInputs(mutableTransaction, feeRate, senderPay)

        val inputsTotalValue = mutableTransaction.inputsToSign.map { it.previousOutput.value }.sum()
        val outputsTotalValue = mutableTransaction.recipientValue + mutableTransaction.changeValue

        return inputsTotalValue - outputsTotalValue
    }

    private fun sampleAddress(): String {
        return addressConverter.convert(publicKey = publicKeyManager.changePublicKey(), scriptType = changeScriptType).string
    }
}
