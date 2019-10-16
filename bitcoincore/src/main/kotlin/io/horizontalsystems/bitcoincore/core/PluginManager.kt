package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.blocks.BlockMedianTimeHelper
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.scripts.Script
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

class PluginManager(private val addressConverter: IAddressConverter, val storage: IStorage, val blockMedianTimeHelper: BlockMedianTimeHelper) {
    private val plugins = mutableMapOf<Int, IPlugin>()

    fun processOutputs(mutableTransaction: MutableTransaction, extraData: Map<String, Map<String, Any>>) {
        plugins.forEach {
            it.value.processOutputs(mutableTransaction, extraData, addressConverter)
        }
    }

    fun addPlugin(plugin: IPlugin) {
        plugins[plugin.id] = plugin
    }

    fun processTransactionWithNullData(transaction: FullTransaction, nullDataOutput: TransactionOutput) {
        val script = Script(nullDataOutput.lockingScript)
        val nullDataChunksIterator = script.chunks.iterator()

        // the first byte OP_RETURN
        nullDataChunksIterator.next()

        while (nullDataChunksIterator.hasNext()) {
            val pluginId = nullDataChunksIterator.next()
            val plugin = plugins[pluginId.opcode] ?: break

            plugin.processTransactionWithNullData(transaction, nullDataChunksIterator, storage, addressConverter)
        }
    }

    fun isSpendable(output: TransactionOutput): Boolean {
        val plugin = plugins[output.pluginId] ?: return true

        val blockMedianTime = blockMedianTimeHelper.medianTimePast ?: return false

        return plugin.isSpendable(output, blockMedianTime)
    }

    fun getTransactionLockTime(transaction: MutableTransaction): Long? {
        val lockTimes = transaction.inputsToSign.mapNotNull { inputToSign ->
            plugins[inputToSign.previousOutput.pluginId]?.getTransactionLockTime(inputToSign.previousOutput)
        }

        return lockTimes.max()
    }

    fun parsePluginData(output: TransactionOutput): Map<String, Map<String, Any>>? {
        val plugin = plugins[output.pluginId] ?: return null

        return mapOf("hodler" to plugin.parsePluginData(output))
    }

}

interface IPlugin {
    val id: Int

    fun processOutputs(mutableTransaction: MutableTransaction, extraData: Map<String, Map<String, Any>>, addressConverter: IAddressConverter)
    fun processTransactionWithNullData(transaction: FullTransaction, nullDataChunks: Iterator<Script.Chunk>, storage: IStorage, addressConverter: IAddressConverter)
    fun isSpendable(output: TransactionOutput, blockMedianTime: Long): Boolean
    fun getTransactionLockTime(output: TransactionOutput): Long
    fun parsePluginData(output: TransactionOutput): Map<String, Any>
}

class InvalidPluginDataException : Exception()
