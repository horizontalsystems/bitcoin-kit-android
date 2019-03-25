package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.crypto.BloomFilter
import io.horizontalsystems.bitcoinkit.storage.OutputWithPublicKey
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoinkit.utils.Utils

class BloomFilterManager(private val storage: IStorage) {

    object BloomFilterExpired : Exception()

    interface Listener {
        fun onFilterUpdated(bloomFilter: BloomFilter)
    }

    var listener: Listener? = null
    var bloomFilter: BloomFilter? = null

    init {
        regenerateBloomFilter()
    }

    fun regenerateBloomFilter() {
        val elements = mutableListOf<ByteArray>()

        for (publicKey in storage.getPublicKeys()) {
            elements.add(publicKey.publicKeyHash)
            elements.add(publicKey.publicKey)
            elements.add(publicKey.scriptHashP2WPKH)
        }

        var transactionOutputs = storage.getOutputsWithPublicKeys().filter {
            it.output.scriptType == ScriptType.P2WPKH || it.output.scriptType == ScriptType.P2PK
        }

        storage.lastBlock()?.height?.let { bestBlockHeight ->
            transactionOutputs = transactionOutputs.filter { needToSetToBloomFilter(it, bestBlockHeight) }
        }

        for (output in transactionOutputs) {
            output.output.transaction(storage)?.let { transaction ->
                val outpoint = transaction.hash + Utils.intToByteArray(output.output.index).reversedArray()
                elements.add(outpoint)
            }
        }

        if (elements.isNotEmpty()) {
            BloomFilter(elements).let {
                if (it != bloomFilter) {
                    bloomFilter = it
                    listener?.onFilterUpdated(it)
                }
            }
        }
    }

    /**
     * @return false if transaction output is spent more then 100 blocks before, otherwise true
     */
    private fun needToSetToBloomFilter(output: OutputWithPublicKey, bestBlockHeight: Int): Boolean {
        val inputs = storage.getInputsWithBlock(output.output)
        if (inputs.isEmpty()) {
            return true
        }

        val outputSpentBlockHeight = inputs.firstOrNull()?.block?.height
        if (outputSpentBlockHeight != null) {
            return bestBlockHeight - outputSpentBlockHeight < 100
        }

        return true
    }

}
