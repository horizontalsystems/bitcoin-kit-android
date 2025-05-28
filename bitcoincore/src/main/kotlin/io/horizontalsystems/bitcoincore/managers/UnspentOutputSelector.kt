package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.DustCalculator
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

class UnspentOutputSelector(
    private val calculator: TransactionSizeCalculator,
    private val dustCalculator: DustCalculator,
    private val unspentOutputProvider: IUnspentOutputProvider,
    private val outputsLimit: Int? = null
) : IUnspentOutputSelector {

    fun getAll(filters: UtxoFilters): List<UnspentOutput> {
        return unspentOutputProvider.getSpendableUtxo(filters)
    }

    @Throws(SendValueErrors::class)
    override fun select(
        value: Long,
        memo: String?,
        feeRate: Int,
        outputScriptType: ScriptType,
        changeType: ScriptType,
        senderPay: Boolean,
        pluginDataOutputSize: Int,
        dustThreshold: Int?,
        changeToFirstInput: Boolean,
        filters: UtxoFilters
    ): SelectedUnspentOutputInfo {
        val sortedOutputs =
            unspentOutputProvider.getSpendableUtxo(filters).sortedWith(compareByDescending<UnspentOutput> {
                it.output.failedToSpend
            }.thenBy {
                it.output.value
            })

        // check if value is not dust. recipientValue may be less, but not more
        if (value < dustCalculator.dust(outputScriptType, dustThreshold)) {
            throw SendValueErrors.Dust
        }

        val params = UnspentOutputQueue.Parameters(
            value = value,
            senderPay = senderPay,
            memo = memo,
            fee = feeRate,
            outputsLimit = outputsLimit,
            outputScriptType = outputScriptType,
            changeType = changeType,
            pluginDataOutputSize = pluginDataOutputSize,
            dustThreshold = dustThreshold,
            changeToFirstInput = changeToFirstInput,
        )
        val queue = UnspentOutputQueue(params, calculator, dustCalculator)

        // select unspentOutputs with the least value until we get the needed value
        var lastError: SendValueErrors? = null
        for (unspentOutput in sortedOutputs) {
            queue.push(unspentOutput)

            try {
                return queue.calculate()
            } catch (error: SendValueErrors) {
                lastError = error
            }
        }
        throw lastError ?: SendValueErrors.InsufficientUnspentOutputs
    }

}
