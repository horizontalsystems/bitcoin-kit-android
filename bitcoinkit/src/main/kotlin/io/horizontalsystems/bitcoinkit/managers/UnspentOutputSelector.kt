package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.models.TransactionOutput
import io.horizontalsystems.bitcoinkit.scripts.ScriptType.P2PKH
import io.horizontalsystems.bitcoinkit.transactions.TransactionSizeCalculator

class UnspentOutputSelector(private val calculator: TransactionSizeCalculator) {

    class EmptyUnspentOutputs : Exception()
    class InsufficientUnspentOutputs : Exception()

    fun select(value: Int, feeRate: Int, outputType: Int = P2PKH, changeType: Int = P2PKH, senderPay: Boolean, outputs: List<TransactionOutput>): SelectedUnspentOutputInfo {

        if (outputs.isEmpty()) {
            throw EmptyUnspentOutputs()
        }

        val dust = (calculator.inputSize(changeType) + calculator.outputSize(changeType)) * feeRate

        //  try to find 1 unspent output with exactly matching value
        for (output in outputs) {
            val fee = calculator.transactionSize(listOf(output.scriptType), listOf(outputType)) * feeRate
            val totalFee = if (senderPay) fee else 0

            if (value + totalFee <= output.value && value + totalFee + dust > output.value) {
                return SelectedUnspentOutputInfo(
                        outputs = listOf(output),
                        totalValue = output.value,
                        fee = if (senderPay) output.value.toInt() - value else fee,
                        addChangeOutput = false)
            }
        }

        //  select outputs with least value until we get needed value
        val sortedOutputs = outputs.sortedBy { it.value }
        val selectedOutputs = mutableListOf<TransactionOutput>()
        val selectedOutputTypes = mutableListOf<Int>()
        var totalValue = 0L

        var fee = 0
        var lastCalculatedFee = 0
        for (output in sortedOutputs) {
            lastCalculatedFee = calculator.transactionSize(inputs = selectedOutputTypes, outputs = listOf(outputType)) * feeRate
            if (senderPay) {
                fee = lastCalculatedFee
            }

            if (totalValue >= value + fee) {
                break
            }

            selectedOutputs.add(output)
            selectedOutputTypes.add(output.scriptType)
            totalValue += output.value
        }

        // if all outputs are selected and total value less than needed throw error
        if (totalValue < value + fee) {
            throw InsufficientUnspentOutputs()
        }

        //  if total selected outputs value more than value and fee for transaction with change output + change input -> add fee for change output and mark as need change address
        var addChangeOutput = false
        if (totalValue > value + lastCalculatedFee + (if (senderPay) dust else 0)) {
            lastCalculatedFee = calculator.transactionSize(inputs = selectedOutputTypes, outputs = listOf(outputType, changeType)) * feeRate
            addChangeOutput = true
        } else if (senderPay) {
            lastCalculatedFee = totalValue.toInt() - value
        }

        return SelectedUnspentOutputInfo(selectedOutputs, totalValue, lastCalculatedFee, addChangeOutput)
    }
}

data class SelectedUnspentOutputInfo(
        val outputs: List<TransactionOutput>,
        val totalValue: Long,
        val fee: Int,
        val addChangeOutput: Boolean
)
