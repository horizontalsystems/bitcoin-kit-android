package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

interface IUnspentOutputSelector {
    fun select(value: Long, feeRate: Int, outputType: ScriptType = ScriptType.P2PKH, changeType: ScriptType = ScriptType.P2PKH, senderPay: Boolean, dust: Int, pluginDataOutputSize: Int): SelectedUnspentOutputInfo
}

data class SelectedUnspentOutputInfo(
        val outputs: List<UnspentOutput>,
        val recipientValue: Long,
        val changeValue: Long?)

sealed class SendValueErrors : Exception() {
    object Dust : SendValueErrors()
    object EmptyOutputs : SendValueErrors()
    object InsufficientUnspentOutputs : SendValueErrors()
    object NoSingleOutput : SendValueErrors()
    object HasOutputFailedToSpend : SendValueErrors()
}

class UnspentOutputSelectorChain : IUnspentOutputSelector {
    private val concreteSelectors = mutableListOf<IUnspentOutputSelector>()

    override fun select(value: Long, feeRate: Int, outputType: ScriptType, changeType: ScriptType, senderPay: Boolean, dust: Int, pluginDataOutputSize: Int): SelectedUnspentOutputInfo {
        var lastError: SendValueErrors? = null

        for (selector in concreteSelectors) {
            try {
                return selector.select(value, feeRate, outputType, changeType, senderPay, dust, pluginDataOutputSize)
            } catch (e: SendValueErrors) {
                lastError = e
            }
        }

        throw lastError ?: Error()
    }

    fun prependSelector(unspentOutputSelector: IUnspentOutputSelector) {
        concreteSelectors.add(0, unspentOutputSelector)
    }
}
