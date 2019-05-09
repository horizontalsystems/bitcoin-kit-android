package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator

class UnspentOutputSelector(private val calculator: TransactionSizeCalculator, private val unspentOutputProvider: IUnspentOutputProvider, private val outputsLimit: Int? = null) : IUnspentOutputSelector {

    override fun select(value: Long, feeRate: Int, outputType: Int, changeType: Int, senderPay: Boolean): SelectedUnspentOutputInfo {
        val unspentOutputs = unspentOutputProvider.getUnspentOutputs()

        if (unspentOutputs.isEmpty()) {
            throw UnspentOutputSelectorError.EmptyUnspentOutputs
        }

        val dust = (calculator.inputSize(changeType) + calculator.outputSize(changeType)) * feeRate

        //  select outputs with least value until we get needed value
        val sortedOutputs = unspentOutputs.sortedBy { it.output.value }
        val selectedOutputs = mutableListOf<UnspentOutput>()
        val selectedOutputTypes = mutableListOf<Int>()
        var totalValue = 0L

        var fee = 0L
        var lastCalculatedFee = 0L
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

            lastCalculatedFee = calculator.transactionSize(inputs = selectedOutputTypes, outputs = listOf(outputType)) * feeRate
            if (senderPay) {
                fee = lastCalculatedFee
            }

            if (totalValue >= lastCalculatedFee && totalValue >= value + fee) {
                break
            }
        }

        // if all outputs are selected and total value less than needed throw error
        if (totalValue < value + fee) {
            throw UnspentOutputSelectorError.InsufficientUnspentOutputs(fee)
        }

        //  if total selected outputs value more than value and fee for transaction with change output + change input -> add fee for change output and mark as need change address
        var addChangeOutput = false
        if (totalValue > value + lastCalculatedFee + (if (senderPay) dust else 0)) {
            lastCalculatedFee = calculator.transactionSize(inputs = selectedOutputTypes, outputs = listOf(outputType, changeType)) * feeRate
            addChangeOutput = true
        } else if (senderPay) {
            lastCalculatedFee = totalValue - value
        }

        return SelectedUnspentOutputInfo(selectedOutputs, totalValue, lastCalculatedFee, addChangeOutput)
    }
}
