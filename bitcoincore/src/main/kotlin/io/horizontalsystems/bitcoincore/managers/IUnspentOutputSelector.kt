package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

interface IUnspentOutputSelector {
    fun select(value: Long, feeRate: Int, outputType: Int = ScriptType.P2PKH, changeType: Int = ScriptType.P2PKH, senderPay: Boolean): SelectedUnspentOutputInfo
}

data class SelectedUnspentOutputInfo(
        val outputs: List<UnspentOutput>,
        val totalValue: Long,
        val fee: Long,
        val addChangeOutput: Boolean
)

sealed class UnspentOutputSelectorError : Exception() {
    object EmptyUnspentOutputs : UnspentOutputSelectorError()
    class InsufficientUnspentOutputs(val fee: Long) : UnspentOutputSelectorError()
}

class UnspentOutputSelectorChain : IUnspentOutputSelector {
    private val concreteSelectors = mutableListOf<IUnspentOutputSelector>()

    override fun select(value: Long, feeRate: Int, outputType: Int, changeType: Int, senderPay: Boolean): SelectedUnspentOutputInfo {
        var lastError: UnspentOutputSelectorError? = null

        for (selector in concreteSelectors) {
            try {
                return selector.select(value, feeRate, outputType, changeType, senderPay)
            } catch (e: UnspentOutputSelectorError) {
                lastError = e
            }
        }

        throw lastError ?: Error()
    }

    fun prependSelector(unspentOutputSelector: IUnspentOutputSelector) {
        concreteSelectors.add(0, unspentOutputSelector)
    }
}
