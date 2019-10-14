package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator

class UnspentOutputSelectorSingleNoChange(private val calculator: TransactionSizeCalculator, private val unspentOutputProvider: IUnspentOutputProvider) : IUnspentOutputSelector {

    override fun select(value: Long, feeRate: Int, outputType: Int, changeType: Int, senderPay: Boolean, pluginDataOutputSize: Int): SelectedUnspentOutputInfo {
        val unspentOutputs = unspentOutputProvider.getSpendableUtxo()

        if (unspentOutputs.isEmpty()) {
            throw UnspentOutputSelectorError.EmptyUnspentOutputs
        }

        val dust = (calculator.inputSize(changeType) + calculator.outputSize(changeType)) * feeRate

        //  try to find 1 unspent output with exactly matching value
        for (unspentOutput in unspentOutputs) {
            val output = unspentOutput.output
            val fee = calculator.transactionSize(listOf(output.scriptType), listOf(outputType), pluginDataOutputSize) * feeRate
            val totalFee = if (senderPay) fee else 0

            if (value + totalFee <= output.value && value + totalFee + dust > output.value) {
                return SelectedUnspentOutputInfo(
                        outputs = listOf(unspentOutput),
                        totalValue = output.value,
                        fee = if (senderPay) output.value - value else fee,
                        addChangeOutput = false)
            }
        }

        throw UnspentOutputSelectorError.InsufficientUnspentOutputs(0)
    }
}
