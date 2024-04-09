package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.core.IRecipientSetter
import io.horizontalsystems.bitcoincore.core.PluginManager
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

class RecipientSetter(
        private val addressConverter: IAddressConverter,
        private val pluginManager: PluginManager
) : IRecipientSetter {

    override fun setRecipient(
        mutableTransaction: MutableTransaction,
        toAddress: String,
        value: Long,
        pluginData: Map<Byte, IPluginData>,
        skipChecking: Boolean,
        memo: String?
    ) {
        mutableTransaction.recipientAddress = addressConverter.convert(toAddress)
        mutableTransaction.recipientValue = value
        mutableTransaction.memo = memo

        pluginManager.processOutputs(mutableTransaction, pluginData, skipChecking)
    }

}
