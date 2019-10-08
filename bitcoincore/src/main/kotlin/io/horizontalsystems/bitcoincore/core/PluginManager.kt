package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptBuilder
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

class PluginManager(private val scriptBuilder: ScriptBuilder, private val addressConverter: IAddressConverter, val storage: IStorage) {
    private val plugins = mutableListOf<IPlugin>()

    fun processOutputs(mutableTransaction: MutableTransaction, extraData: Map<String, Map<String, Any>>) {
        plugins.forEach {
            it.processOutputs(mutableTransaction, extraData, scriptBuilder, addressConverter)
        }
    }

    fun addPlugin(plugin: IPlugin) {
        plugins.add(plugin)
    }

    fun processTransactionWithNullData(transaction: FullTransaction, nullDataOutput: TransactionOutput) {
        plugins.forEach {
            it.processTransactionWithNullData(transaction, nullDataOutput, storage)
        }
    }

}

interface IPlugin {
    fun processOutputs(mutableTransaction: MutableTransaction, extraData: Map<String, Map<String, Any>>, scriptBuilder: ScriptBuilder, addressConverter: IAddressConverter)
    fun processTransactionWithNullData(transaction: FullTransaction, nullDataOutput: TransactionOutput, storage: IStorage)
}
