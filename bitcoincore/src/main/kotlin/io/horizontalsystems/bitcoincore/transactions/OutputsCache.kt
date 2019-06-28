package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.core.HashBytes
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput

class OutputsCache {
    private val outputsCache = mutableMapOf<HashBytes, MutableList<Int>>()

    fun add(outputs: List<TransactionOutput>) {
        for (output in outputs) {
            if (output.publicKeyPath != null) {
                val out = outputsCache[HashBytes(output.transactionHash)]
                if (out == null) {
                    outputsCache[HashBytes(output.transactionHash)] = mutableListOf(output.index)
                } else {
                    outputsCache[HashBytes(output.transactionHash)]?.add(output.index)
                }
            }
        }
    }

    fun hasOutputs(inputs: List<TransactionInput>): Boolean {
        for (input in inputs) {
            val outputIndices = outputsCache[HashBytes(input.previousOutputTxHash)] ?: continue
            if (outputIndices.contains(input.previousOutputIndex.toInt())) {
                return true
            }
        }

        return false
    }

    companion object {
        fun create(storage: IStorage): OutputsCache {
            val outputsCache = OutputsCache()
            val outputs = storage.getMyOutputs()

            outputsCache.add(outputs)

            return outputsCache
        }
    }
}
