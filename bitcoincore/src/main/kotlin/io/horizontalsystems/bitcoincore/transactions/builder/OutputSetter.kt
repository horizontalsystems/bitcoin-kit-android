package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.core.PluginManager
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

class OutputSetter(private val addressConverter: IAddressConverter, private val pluginManager: PluginManager) {

    fun setOutputs(mutableTransaction: MutableTransaction, addressStr: String, value: Long, pluginData: Map<Byte, IPluginData>) {
        mutableTransaction.recipientAddress = addressConverter.convert(addressStr)
        mutableTransaction.recipientValue = value

        pluginManager.processOutputs(mutableTransaction, pluginData)
    }

}
