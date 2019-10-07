package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptBuilder
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

class PluginManager(private val scriptBuilder: ScriptBuilder, private val addressConverter: IAddressConverter) {
    private val plugins = mutableListOf<IPlugin>()

    fun processOutputs(mutableTransaction: MutableTransaction, extraData: Map<String, Map<String, Any>>) {
        plugins.forEach {
            it.processOutputs(mutableTransaction, extraData, scriptBuilder, addressConverter)
        }
    }

    fun addPlugin(plugin: IPlugin) {
        plugins.add(plugin)
    }

}

interface IPlugin {
    fun processOutputs(mutableTransaction: MutableTransaction, extraData: Map<String, Map<String, Any>>, scriptBuilder: ScriptBuilder, addressConverter: IAddressConverter)
}
