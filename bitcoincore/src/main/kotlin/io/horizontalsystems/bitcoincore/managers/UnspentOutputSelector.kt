package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator

class UnspentOutputSelector(private val calculator: TransactionSizeCalculator, private val unspentOutputProvider: IUnspentOutputProvider, private val outputsLimit: Int? = null) : IUnspentOutputSelector {

    override fun select(value: Long, feeRate: Int, outputType: Int, changeType: Int, senderPay: Boolean, dust: Int, pluginDataOutputSize: Int): SelectedUnspentOutputInfo {
        if (value <= dust) {
            throw SendValueErrors.Dust
        }

        val unspentOutputs = unspentOutputProvider.getSpendableUtxo()

        if (unspentOutputs.isEmpty()) {
            throw SendValueErrors.EmptyOutputs
        }

        //  select outputs with least value until we get needed value
        val sortedOutputs = unspentOutputs.sortedBy { it.output.value }
        val selectedOutputs = mutableListOf<UnspentOutput>()
        val selectedOutputTypes = mutableListOf<Int>()
        var totalValue = 0L
        var recipientValue = 0L
        var sentValue = 0L
        var fee = 0L

        for (unspentOutput in sortedOutputs) {
            selectedOutputs.add(unspentOutput)
            selectedOutputTypes.add(unspentOutput.output.scriptType)
            totalValue += unspentOutput.output.value

            outputsLimit?.let {
                if (selectedOutputs.size > it) {
                    val outputToExclude = selectedOutputs.first()
                    selectedOutputs.removeAt(0)
                    selectedOutputTypes.removeAt(0)
                    totalValue -= outputToExclude.output.value
                }
            }

            fee = calculator.transactionSize(selectedOutputTypes, listOf(outputType), pluginDataOutputSize) * feeRate

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
            throw SendValueErrors.InsufficientUnspentOutputs(if (senderPay) fee else 0)
        }

        val changeOutputHavingTransactionFee = calculator.transactionSize(selectedOutputTypes, listOf(outputType, changeType), pluginDataOutputSize) * feeRate
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
