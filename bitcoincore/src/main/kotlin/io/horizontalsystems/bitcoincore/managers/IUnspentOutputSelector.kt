package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

interface IUnspentOutputSelector {
    fun select(
        value: Long,
        memo: String?,
        feeRate: Int,
        outputScriptType: ScriptType = ScriptType.P2PKH,
        changeType: ScriptType = ScriptType.P2PKH,
        senderPay: Boolean,
        pluginDataOutputSize: Int,
        changeToFirstInput: Boolean,
        filters: UtxoFilters
    ): SelectedUnspentOutputInfo
}

data class SelectedUnspentOutputInfo(
        val outputs: List<UnspentOutput>,
        val recipientValue: Long,
        val changeValue: Long?
)

sealed class SendValueErrors : Exception() {
    object Dust : SendValueErrors()
    object EmptyOutputs : SendValueErrors()
    object InsufficientUnspentOutputs : SendValueErrors()
    object NoSingleOutput : SendValueErrors()
    object HasOutputFailedToSpend : SendValueErrors()
}

class UnspentOutputSelectorChain(private val unspentOutputProvider: IUnspentOutputProvider) : IUnspentOutputSelector {
    private val concreteSelectors = mutableListOf<IUnspentOutputSelector>()

    fun getAllSpendable(filters: UtxoFilters): List<UnspentOutput> {
        return unspentOutputProvider.getSpendableUtxo(filters)
    }

    override fun select(
        value: Long,
        memo: String?,
        feeRate: Int,
        outputScriptType: ScriptType,
        changeType: ScriptType,
        senderPay: Boolean,
        pluginDataOutputSize: Int,
        changeToFirstInput: Boolean,
        filters: UtxoFilters
    ): SelectedUnspentOutputInfo {
        var lastError: SendValueErrors? = null

        for (selector in concreteSelectors) {
            try {
                return selector.select(
                    value,
                    memo,
                    feeRate,
                    outputScriptType,
                    changeType,
                    senderPay,
                    pluginDataOutputSize,
                    changeToFirstInput,
                    filters
                )
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
