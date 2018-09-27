package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.models.TransactionOutput
import bitcoin.wallet.kit.scripts.ScriptType
import bitcoin.wallet.kit.transactions.TransactionSizeCalculator

class UnspentOutputSelector(private val txSizeCalculator: TransactionSizeCalculator) {

    class EmptyUnspentOutputs : Exception()
    class InsufficientUnspentOutputs : Exception()

    fun select(value: Int, feeRate: Int, scriptType: Int, senderPay: Boolean, unspentOutputs: List<TransactionOutput>): SelectedUnspentOutputInfo {

        if (unspentOutputs.isEmpty()) {
            throw EmptyUnspentOutputs()
        }

        val selected = mutableListOf<TransactionOutput>()
        var calculatedFee = (txSizeCalculator.emptyTxSize + txSizeCalculator.outputSize(scriptType)) * feeRate
        val dust = txSizeCalculator.inputSize(ScriptType.P2PKH) * feeRate

        // try to find 1 unspent output with exactly matching value
        unspentOutputs.firstOrNull {
            val totalFee = if (senderPay) calculatedFee + txSizeCalculator.inputSize(it.scriptType) * feeRate else 0
            (value + totalFee <= it.value) && (value + totalFee + dust > it.value)               //value + input fee + dust
        }?.let { output ->
            selected.add(output)
            calculatedFee += txSizeCalculator.inputSize(output.scriptType) * feeRate

            return SelectedUnspentOutputInfo(outputs = selected, totalValue = output.value, fee = calculatedFee)
        }

        // select outputs with least value until we get needed value
        val sortedOutputs = unspentOutputs.sortedBy { it.value }
        var totalValue = 0L
        for (output in sortedOutputs) {
            if (totalValue >= value + (if (senderPay) calculatedFee else 0)) {
                break
            }
            selected.add(output)
            calculatedFee += txSizeCalculator.inputSize(output.scriptType) * feeRate
            totalValue += output.value
        }

        // if all outputs are selected and total value less than needed throw error
        if (totalValue < value + (if (senderPay) calculatedFee else 0)) {
            throw InsufficientUnspentOutputs()
        }

        return SelectedUnspentOutputInfo(outputs = selected, totalValue = totalValue, fee = calculatedFee)
    }

}

data class SelectedUnspentOutputInfo(val outputs: List<TransactionOutput>, val totalValue: Long, val fee: Int)

