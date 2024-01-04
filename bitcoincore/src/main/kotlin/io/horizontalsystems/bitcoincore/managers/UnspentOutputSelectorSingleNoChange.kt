package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.DustCalculator
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

class UnspentOutputSelectorSingleNoChange(
    private val calculator: TransactionSizeCalculator,
    private val dustCalculator: DustCalculator,
    private val unspentOutputProvider: IUnspentOutputProvider
) : IUnspentOutputSelector {

    override fun select(
        value: Long,
        feeRate: Int,
        outputScriptType: ScriptType,
        changeType: ScriptType,
        senderPay: Boolean,
        pluginDataOutputSize: Int
    ): SelectedUnspentOutputInfo {
        val dust = dustCalculator.dust(outputScriptType)
        if (value <= dust) {
            throw SendValueErrors.Dust
        }

        val sortedOutputs =
            unspentOutputProvider.getSpendableUtxo().sortedWith(compareByDescending<UnspentOutput> {
                it.output.failedToSpend
            }.thenBy {
                it.output.value
            })

        if (sortedOutputs.isEmpty()) {
            throw SendValueErrors.EmptyOutputs
        }

        if (sortedOutputs.any { it.output.failedToSpend }) {
            throw SendValueErrors.HasOutputFailedToSpend
        }

        val params = UnspentOutputQueue.Parameters(
            value = value,
            senderPay = senderPay,
            fee = feeRate,
            outputsLimit = null,
            outputScriptType = outputScriptType,
            changeType = changeType,
            pluginDataOutputSize = pluginDataOutputSize
        )
        val queue = UnspentOutputQueue(params, calculator, dustCalculator)

        //  try to find 1 unspent output with exactly matching value
        for (unspentOutput in sortedOutputs) {
            queue.set(listOf(unspentOutput))

            try {
                val info = queue.calculate()
                if (info.changeValue == null) {
                    return info
                }
            } catch (error: SendValueErrors) {
                //  ignore
            }
        }

        throw SendValueErrors.NoSingleOutput
    }
}
