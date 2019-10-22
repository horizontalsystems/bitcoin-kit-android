package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.blocks.BlockMedianTimeHelper
import io.horizontalsystems.bitcoincore.managers.IRestoreKeyConverter
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.scripts.Script
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

class PluginManager(private val addressConverter: IAddressConverter, val storage: IStorage, private val blockMedianTimeHelper: BlockMedianTimeHelper) : IRestoreKeyConverter {
    private val plugins = mutableMapOf<Byte, IPlugin>()

    fun processOutputs(mutableTransaction: MutableTransaction, pluginData: Map<Byte, Map<String, Any>>) {
        pluginData.forEach {
            val plugin = checkNotNull(plugins[it.key])
            plugin.processOutputs(mutableTransaction, it.value, addressConverter)
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
                plugin.processTransactionWithNullData(transaction, nullDataChunksIterator, storage, addressConverter)
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
        return plugin.isSpendable(unspentOutput, blockMedianTimeHelper)
    }

    fun parsePluginData(output: TransactionOutput): Map<Byte, Map<String, Any>>? {
        val plugin = plugins[output.pluginId] ?: return null

        return try {
            mapOf(plugin.id to plugin.parsePluginData(output))
        } catch (e: Exception) {
            null
        }
    }

    override fun keysForApiRestore(publicKey: PublicKey): List<String> {
        return plugins.map { it.value.keysForApiRestore(publicKey, addressConverter) }.flatten().distinct()
    }

    override fun bloomFilterElements(publicKey: PublicKey): List<ByteArray> {
        return listOf()
    }
}

interface IPlugin {
    val id: Byte

    fun processOutputs(mutableTransaction: MutableTransaction, pluginData: Map<String, Any>, addressConverter: IAddressConverter)
    fun processTransactionWithNullData(transaction: FullTransaction, nullDataChunks: Iterator<Script.Chunk>, storage: IStorage, addressConverter: IAddressConverter)
    fun isSpendable(unspentOutput: UnspentOutput, blockMedianTimeHelper: BlockMedianTimeHelper): Boolean
    fun getInputSequence(output: TransactionOutput): Long
    fun parsePluginData(output: TransactionOutput): Map<String, Any>
    fun keysForApiRestore(publicKey: PublicKey, addressConverter: IAddressConverter): List<String>
}
