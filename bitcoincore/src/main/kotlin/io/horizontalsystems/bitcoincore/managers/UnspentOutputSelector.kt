package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.DustCalculator
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

class UnspentOutputSelector(
    private val calculator: TransactionSizeCalculator,
    private val dustCalculator: DustCalculator,
    private val unspentOutputProvider: IUnspentOutputProvider,
    private val outputsLimit: Int? = null
) : IUnspentOutputSelector {

    val all: List<UnspentOutput>
        get() = unspentOutputProvider.getSpendableUtxo()

    @Throws(SendValueErrors::class)
    override fun select(
        value: Long,
        feeRate: Int,
        outputScriptType: ScriptType,
        changeType: ScriptType,
        senderPay: Boolean,
        pluginDataOutputSize: Int
    ): SelectedUnspentOutputInfo {
        val sortedOutputs =
            unspentOutputProvider.getSpendableUtxo().sortedWith(compareByDescending<UnspentOutput> {
                it.output.failedToSpend
            }.thenBy {
                it.output.value
            })

        // check if value is not dust. recipientValue may be less, but not more
        if (value < dustCalculator.dust(outputScriptType)) {
            throw SendValueErrors.Dust
        }

        val params = UnspentOutputQueue.Parameters(
            value = value,
            senderPay = senderPay,
            fee = feeRate,
            outputsLimit = outputsLimit,
            outputScriptType = outputScriptType,
            changeType = changeType,
            pluginDataOutputSize = pluginDataOutputSize
        )
        val queue = UnspentOutputQueue(params, calculator, dustCalculator)

        // select unspentOutputs with the least value until we get the needed value
        var lastError: Error? = null
        for (unspentOutput in sortedOutputs) {
            queue.push(unspentOutput)

            try {
                return queue.calculate()
            } catch (error: Error) {
                lastError = error
            }
        }
        throw lastError ?: SendValueErrors.InsufficientUnspentOutputs
    }

}
