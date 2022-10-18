package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

class UnspentOutputSelectorSingleNoChange(private val calculator: TransactionSizeCalculator, private val unspentOutputProvider: IUnspentOutputProvider) : IUnspentOutputSelector {

    override fun select(value: Long, feeRate: Int, outputType: ScriptType, changeType: ScriptType, senderPay: Boolean, dust: Int, pluginDataOutputSize: Int): SelectedUnspentOutputInfo {
        if (value <= dust) {
            throw SendValueErrors.Dust
        }

        val unspentOutputs = unspentOutputProvider.getSpendableUtxo()
        return selectOutputs(unspentOutputs, value, feeRate, outputType, changeType, senderPay, dust, pluginDataOutputSize)
    }

    override fun select(
        unspentOutputAddresses: List<String>,
        value: Long,
        feeRate: Int,
        outputType: ScriptType,
        changeType: ScriptType,
        senderPay: Boolean,
        dust: Int,
        pluginDataOutputSize: Int
    ): SelectedUnspentOutputInfo {
        if (value <= dust) {
            throw SendValueErrors.Dust
        }


        val spendableUTXOs = unspentOutputProvider.getSpendableUtxo()
        val unspentOutputs = unspentOutputAddresses.map { spendableUTXOs.find { utxo -> utxo.output.address == it }!! }

        return selectOutputs(unspentOutputs, value, feeRate, outputType, changeType, senderPay, dust, pluginDataOutputSize)
    }

    private fun selectOutputs(
        unspentOutputs: List<UnspentOutput>,
        value: Long,
        feeRate: Int,
        outputType: ScriptType,
        changeType: ScriptType,
        senderPay: Boolean,
        dust: Int,
        pluginDataOutputSize: Int
    ): SelectedUnspentOutputInfo {
        if (value <= dust) {
            throw SendValueErrors.Dust
        }

        if (unspentOutputs.isEmpty()) {
            throw SendValueErrors.EmptyOutputs
        }

        if (unspentOutputs.any { it.output.failedToSpend }) {
            throw SendValueErrors.HasOutputFailedToSpend
        }

        //  try to find 1 unspent output with exactly matching value
        for (unspentOutput in unspentOutputs) {
            val output = unspentOutput.output
            val fee = calculator.transactionSize(listOf(output), listOf(outputType), pluginDataOutputSize) * feeRate

            val recipientValue = if (senderPay) value else value - fee
            val sentValue = if (senderPay) value + fee else value

            if (sentValue <= output.value &&            // output.value is enough
                recipientValue >= dust &&           // receivedValue won't be dust
                output.value - sentValue < dust) {  // no need to add change output

                return SelectedUnspentOutputInfo(listOf(unspentOutput), recipientValue, null)
            }
        }

        throw SendValueErrors.NoSingleOutput
    }
}
