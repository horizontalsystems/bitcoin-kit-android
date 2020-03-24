package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.core.IRecipientSetter
import io.horizontalsystems.bitcoincore.managers.PublicKeyManager
import io.horizontalsystems.bitcoincore.models.TransactionDataSortType
import io.horizontalsystems.bitcoincore.transactions.builder.InputSetter
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.AddressConverterChain

class TransactionFeeCalculator(
        private val recipientSetter: IRecipientSetter,
        private val inputSetter: InputSetter,
        private val addressConverter: AddressConverterChain,
        private val publicKeyManager: PublicKeyManager,
        private val changeScriptType: ScriptType
) {

    fun fee(value: Long, feeRate: Int, senderPay: Boolean, toAddress: String?, pluginData: Map<Byte, IPluginData>): Long {
        val mutableTransaction = MutableTransaction()

        recipientSetter.setRecipient(mutableTransaction, toAddress ?: sampleAddress(), value, pluginData, true)
        inputSetter.setInputs(mutableTransaction, feeRate, senderPay, TransactionDataSortType.None)

        val inputsTotalValue = mutableTransaction.inputsToSign.map { it.previousOutput.value }.sum()
        val outputsTotalValue = mutableTransaction.recipientValue + mutableTransaction.changeValue

        return inputsTotalValue - outputsTotalValue
    }

    private fun sampleAddress(): String {
        return addressConverter.convert(publicKey = publicKeyManager.changePublicKey(), scriptType = changeScriptType).string
    }
}
