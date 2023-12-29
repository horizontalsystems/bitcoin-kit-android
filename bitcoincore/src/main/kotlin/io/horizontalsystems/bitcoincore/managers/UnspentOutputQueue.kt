package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.DustCalculator
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

class UnspentOutputQueue(
    private val parameters: Parameters,
    private val sizeCalculator: TransactionSizeCalculator,
    dustCalculator: DustCalculator,
    outputs: List<UnspentOutput> = emptyList()
) {

    private var selectedOutputs: MutableList<UnspentOutput> = mutableListOf()
    private var totalValue: Long = 0L

    val recipientOutputDust = dustCalculator.dust(parameters.outputScriptType)
    val changeOutputDust = dustCalculator.dust(parameters.changeType)

    init {
        outputs.forEach {
            push(it)
        }
    }

    fun push(output: UnspentOutput) {
        selectedOutputs.add(output)
        totalValue += output.output.value

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
    private fun values(value: Long, total: Long, fee: Long): Pair<Long, Long> {
        val receiveValue = if (parameters.senderPay) value else value - fee
        val sentValue = if (parameters.senderPay) value + fee else value

        if (totalValue < sentValue) {
            throw SendValueErrors.InsufficientUnspentOutputs
        }

        if (receiveValue <= recipientOutputDust) {
            throw SendValueErrors.Dust
        }

        val remainder = total - receiveValue - fee
        return Pair(receiveValue, remainder)
    }

    @Throws(SendValueErrors::class)
    fun calculate(): SelectedUnspentOutputInfo {
        if (selectedOutputs.isEmpty()) {
            throw SendValueErrors.EmptyOutputs
        }

        val feeWithoutChange = sizeCalculator.transactionSize(
            previousOutputs = selectedOutputs.map { it.output },
            outputs = listOf(parameters.outputScriptType),
            pluginDataOutputSize = parameters.pluginDataOutputSize
        ) * parameters.fee

        val sendValues = values(parameters.value, totalValue, feeWithoutChange)

        val changeFee = sizeCalculator.outputSize(parameters.changeType) * parameters.fee
        val remainder = sendValues.second - changeFee

        return if (remainder <= recipientOutputDust) {
            SelectedUnspentOutputInfo(selectedOutputs, sendValues.first, null)
        } else {
            SelectedUnspentOutputInfo(selectedOutputs, sendValues.first, remainder)
        }
    }

    data class Parameters(
        val value: Long,
        val senderPay: Boolean,
        val fee: Int,
        val outputsLimit: Int?,
        val outputScriptType: ScriptType,
        val changeType: ScriptType,
        val pluginDataOutputSize: Int
    )
}