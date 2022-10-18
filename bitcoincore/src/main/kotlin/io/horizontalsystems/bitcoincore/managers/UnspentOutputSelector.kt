package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

class UnspentOutputSelector(private val calculator: TransactionSizeCalculator, private val unspentOutputProvider: IUnspentOutputProvider, private val outputsLimit: Int? = null) : IUnspentOutputSelector {

    override fun select(value: Long, feeRate: Int, outputType: ScriptType, changeType: ScriptType, senderPay: Boolean, dust: Int, pluginDataOutputSize: Int): SelectedUnspentOutputInfo {
        if (value <= dust) {
            throw SendValueErrors.Dust
        }

        val unspentOutputs = unspentOutputProvider.getSpendableUtxo()

        if (unspentOutputs.isEmpty()) {
            throw SendValueErrors.EmptyOutputs
        }

        val sortedOutputs = unspentOutputs.sortedWith(compareByDescending<UnspentOutput> {
            it.output.failedToSpend
        }.thenBy {
            it.output.value
        })

        val selectedOutputs = mutableListOf<UnspentOutput>()
        var totalValue = 0L
        var recipientValue = 0L
        var sentValue = 0L
        var fee: Long

        for (unspentOutput in sortedOutputs) {
            selectedOutputs.add(unspentOutput)
            totalValue += unspentOutput.output.value

            outputsLimit?.let {
                if (selectedOutputs.size > it) {
                    val outputToExclude = selectedOutputs.first()
                    selectedOutputs.removeAt(0)
                    totalValue -= outputToExclude.output.value
                }
            }

            fee = calculator.transactionSize(selectedOutputs.map { it.output }, listOf(outputType), pluginDataOutputSize) * feeRate

            recipientValue = if (senderPay) value else value - fee
            sentValue = if (senderPay) value + fee else value

            if (sentValue <= totalValue) {      // totalValue is enough
                if (recipientValue >= dust) {   // receivedValue won't be dust
                    break
                } else {
                    // Here senderPay is false, because otherwise "dust" exception would throw far above.
                    // Adding more UTXOs will make fee even greater, making recipientValue even less and dust anyway
                    throw SendValueErrors.Dust
                }
            }
        }

        // if all outputs are selected and total value less than needed throw error
        if (totalValue < sentValue) {
            throw SendValueErrors.InsufficientUnspentOutputs
        }

        val changeOutputHavingTransactionFee = calculator.transactionSize(selectedOutputs.map { it.output }, listOf(outputType, changeType), pluginDataOutputSize) * feeRate
        val withChangeRecipientValue = if (senderPay) value else value - changeOutputHavingTransactionFee
        val withChangeSentValue = if (senderPay) value + changeOutputHavingTransactionFee else value
        // if selected UTXOs total value >= recipientValue(toOutput value) + fee(for transaction with change output) + dust(minimum changeOutput value)
        if (totalValue >= withChangeRecipientValue + changeOutputHavingTransactionFee + dust) {
            // totalValue is too much, we must have change output
            if (withChangeRecipientValue <= dust) {
                throw SendValueErrors.Dust
            }

            return SelectedUnspentOutputInfo(selectedOutputs, withChangeRecipientValue, totalValue - withChangeSentValue)
        }

        // No change needed
        return SelectedUnspentOutputInfo(selectedOutputs, recipientValue, null)
    }
}
