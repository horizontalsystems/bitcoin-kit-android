package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.managers.IRestoreKeyConverter
import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.InputWithPreviousOutput
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.scripts.Script

class PluginManager {
    private val plugins = mutableMapOf<Byte, IPlugin>()

    fun processOutputs(mutableTransaction: MutableTransaction, pluginData: Map<Byte, IPluginData>, skipChecking: Boolean) {
        pluginData.forEach {
            val plugin = checkNotNull(plugins[it.key])
            plugin.processOutputs(mutableTransaction, it.value, skipChecking)
        }
    }

    fun processInputs(mutableTransaction: MutableTransaction) {
        for (inputToSign in mutableTransaction.inputsToSign) {
            val pluginId = inputToSign.previousOutput.pluginId ?: continue
            val plugin = checkNotNull(plugins[pluginId])
            inputToSign.input.sequence = plugin.getInputSequence(inputToSign.previousOutput)
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
            val plugin = plugins[pluginId.opcode.toByte()] ?: break

            try {
                plugin.processTransactionWithNullData(transaction, nullDataChunksIterator)
            } catch (e: Exception) {

            }
        }
    }

    /**
     * Tell if UTXO is spendable using the corresponding plugin
     *
     * @return true if pluginId is null, false if no plugin found for pluginId,
     * otherwise delegate it to corresponding plugin
     */
    fun isSpendable(unspentOutput: UnspentOutput): Boolean {
        val pluginId = unspentOutput.output.pluginId ?: return true
        val plugin = plugins[pluginId] ?: return false
        return plugin.isSpendable(unspentOutput)
    }

    fun parsePluginData(output: TransactionOutput, txTimestamp: Long): IPluginOutputData? {
        val plugin = plugins[output.pluginId] ?: return null

        return try {
            plugin.parsePluginData(output, txTimestamp)
        } catch (e: Exception) {
            null
        }
    }

    fun validateAddress(address: Address, pluginData: Map<Byte, IPluginData>) {
        pluginData.forEach {
            val plugin = checkNotNull(plugins[it.key])

            plugin.validateAddress(address)
        }
    }

    fun incrementedSequence(inputWithPreviousOutput: InputWithPreviousOutput): Long {
        val plugin = inputWithPreviousOutput.previousOutput?.pluginId?.let { plugins[it] }
        val sequence = inputWithPreviousOutput.input.sequence

        return plugin?.incrementSequence(sequence) ?: (sequence + 1)
    }
}

interface IPlugin : IRestoreKeyConverter {
    val id: Byte

    fun processOutputs(mutableTransaction: MutableTransaction, pluginData: IPluginData, skipChecking: Boolean)
    fun processTransactionWithNullData(transaction: FullTransaction, nullDataChunks: Iterator<Script.Chunk>)
    fun isSpendable(unspentOutput: UnspentOutput): Boolean
    fun getInputSequence(output: TransactionOutput): Long
    fun parsePluginData(output: TransactionOutput, txTimestamp: Long): IPluginOutputData
    fun validateAddress(address: Address)
    fun incrementSequence(sequence: Long): Long
}

interface IPluginData
interface IPluginOutputData
