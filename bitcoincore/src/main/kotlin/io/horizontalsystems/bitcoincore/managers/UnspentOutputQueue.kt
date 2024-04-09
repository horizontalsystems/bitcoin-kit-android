package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.DustCalculator
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

class UnspentOutputQueue(
    private val parameters: Parameters,
    private val sizeCalculator: TransactionSizeCalculator,
    dustCalculator: DustCalculator,
) {

    private var selectedOutputs: MutableList<UnspentOutput> = mutableListOf()
    private var totalValue: Long = 0L

    val recipientOutputDust = dustCalculator.dust(parameters.outputScriptType)

    fun push(output: UnspentOutput) {
        selectedOutputs.add(output)
        totalValue += output.output.value
        enforceOutputsLimit()
    }

    private fun enforceOutputsLimit() {
        val limit = parameters.outputsLimit
        if (limit != null && limit > 0 && selectedOutputs.size > limit) {
            totalValue -= selectedOutputs.firstOrNull()?.output?.value ?: 0
            selectedOutputs.removeFirst()
        }
    }

    fun set(outputs: List<UnspentOutput>) {
        selectedOutputs.clear()
        totalValue = 0

        outputs.forEach { push(it) }
    }

    @Throws(SendValueErrors::class)
    fun calculate(): SelectedUnspentOutputInfo {
        if (selectedOutputs.isEmpty()) {
            throw SendValueErrors.EmptyOutputs
        }

        val feeWithoutChange = calculateFeeWithoutChange()
        val (receiveValue, remainder) = calculateSendValues(feeWithoutChange)

        val changeFee = sizeCalculator.outputSize(parameters.changeType) * parameters.fee
        val actualRemainder = remainder - changeFee

        return if (actualRemainder <= recipientOutputDust) {
            SelectedUnspentOutputInfo(selectedOutputs, receiveValue, null)
        } else {
            SelectedUnspentOutputInfo(selectedOutputs, receiveValue, actualRemainder)
        }
    }

    private fun calculateFeeWithoutChange(): Long =
        sizeCalculator.transactionSize(
            previousOutputs = selectedOutputs.map { it.output },
            outputs = listOf(parameters.outputScriptType),
            memo = parameters.memo,
            pluginDataOutputSize = parameters.pluginDataOutputSize
        ) * parameters.fee

    @Throws(SendValueErrors::class)
    private fun calculateSendValues(feeWithoutChange: Long): Pair<Long, Long> {
        val sentValue = if (parameters.senderPay) parameters.value + feeWithoutChange else parameters.value

        if (totalValue < sentValue) {
            throw SendValueErrors.InsufficientUnspentOutputs
        }

        val receiveValue = if (parameters.senderPay) parameters.value else parameters.value - feeWithoutChange
        if (receiveValue <= recipientOutputDust) {
            throw SendValueErrors.Dust
        }

        return Pair(receiveValue, totalValue - receiveValue - feeWithoutChange)
    }


    data class Parameters(
        val value: Long,
        val senderPay: Boolean,
        val memo: String?,
        val fee: Int,
        val outputsLimit: Int?,
        val outputScriptType: ScriptType,
        val changeType: ScriptType,
        val pluginDataOutputSize: Int
    )
}