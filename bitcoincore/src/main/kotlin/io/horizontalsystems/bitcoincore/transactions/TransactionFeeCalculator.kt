package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.core.IPublicKeyManager
import io.horizontalsystems.bitcoincore.core.IRecipientSetter
import io.horizontalsystems.bitcoincore.models.BitcoinSendInfo
import io.horizontalsystems.bitcoincore.models.TransactionDataSortType
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.builder.InputSetter
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.AddressConverterChain

class TransactionFeeCalculator(
    private val recipientSetter: IRecipientSetter,
    private val inputSetter: InputSetter,
    private val addressConverter: AddressConverterChain,
    private val publicKeyManager: IPublicKeyManager,
    private val changeScriptType: ScriptType,
) {

    fun sendInfo(
        value: Long,
        feeRate: Int,
        senderPay: Boolean,
        toAddress: String?,
        memo: String?,
        unspentOutputs: List<UnspentOutput>?,
        pluginData: Map<Byte, IPluginData>
    ): BitcoinSendInfo {
        val mutableTransaction = MutableTransaction()

        recipientSetter.setRecipient(
            mutableTransaction = mutableTransaction,
            toAddress = toAddress ?: sampleAddress(),
            value = value,
            pluginData = pluginData,
            skipChecking = true,
            memo = memo
        )

        val outputInfo = inputSetter.setInputs(
            mutableTransaction = mutableTransaction,
            feeRate = feeRate,
            senderPay = senderPay,
            unspentOutputs = unspentOutputs,
            sortType = TransactionDataSortType.None,
            rbfEnabled = false
        )

        val inputsTotalValue = mutableTransaction.inputsToSign.sumOf { it.previousOutput.value }
        val outputsTotalValue = mutableTransaction.recipientValue + mutableTransaction.changeValue

        return BitcoinSendInfo(
            unspentOutputs = outputInfo.unspentOutputs,
            fee = inputsTotalValue - outputsTotalValue,
            changeValue = outputInfo.changeInfo?.value,
            changeAddress = outputInfo.changeInfo?.address
        )
    }

    private fun sampleAddress(): String {
        return addressConverter.convert(
            publicKey = publicKeyManager.changePublicKey(),
            scriptType = changeScriptType
        ).stringValue
    }
}
