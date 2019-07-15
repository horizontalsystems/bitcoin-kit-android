package io.horizontalsystems.bitcoincore

import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.managers.BloomFilterManager
import io.horizontalsystems.bitcoincore.managers.IBloomFilterProvider
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.utils.Utils
import java.util.logging.Logger

class WatchedTransactionManager : IBloomFilterProvider {
    interface Listener {
        fun onTransactionSeenP2SH(tx: FullTransaction, outputIndex: Int) = Unit
        fun onTransactionSeenOutpoint(tx: FullTransaction, inputIndex: Int) = Unit
    }

    override var bloomFilterManager: BloomFilterManager? = null

    private val filters = mutableMapOf<TransactionFilter, Listener>()
    private val logger = Logger.getLogger("Watcher")

    fun add(filter: TransactionFilter, listener: Listener) {
        logger.info("Add filter: $filter")

        filters[filter] = listener
        bloomFilterManager?.regenerateBloomFilter()
    }

    override fun getBloomFilterElements(): List<ByteArray> {
        return filters.keys.map { it.getBloomFilterElement() }
    }

    fun onTransactionReceived(transaction: FullTransaction) {
        filters.forEach {
            val filter = it.key
            val listener = it.value

            when (filter) {
                is TransactionFilter.P2SHOutput -> {
                    transaction.outputs.find { it.keyHash?.contentEquals(filter.scriptHash) == true }?.let { output ->
                        logger.info("Transaction received ${transaction.header.hash.toReversedHex()} for filter: $filter")

                        listener.onTransactionSeenP2SH(transaction, output.index)
                    }
                }
                is TransactionFilter.Outpoint -> {
                    val inputs = transaction.inputs
                    val i = inputs.indexOfFirst { it.previousOutputTxHash.contentEquals(filter.transactionHash) && it.previousOutputIndex == filter.index }
                    inputs.getOrNull(i)?.let {
                        logger.info("Transaction received ${transaction.header.hash.toReversedHex()} for filter: $filter")

                        listener.onTransactionSeenOutpoint(transaction, i)
                    }
                }
            }
        }
    }
}

sealed class TransactionFilter {
    abstract fun getBloomFilterElement() : ByteArray

    class P2SHOutput(val scriptHash: ByteArray) : TransactionFilter() {
        override fun getBloomFilterElement(): ByteArray {
            return scriptHash
        }

        override fun toString(): String {
            return "P2SHOutputFilter(${scriptHash.toHexString()})"
        }
    }

    class Outpoint(val transactionHash: ByteArray, val index: Long) : TransactionFilter() {
        override fun getBloomFilterElement(): ByteArray {
            return transactionHash + Utils.intToByteArray(index.toInt()).reversedArray()
        }

        override fun toString(): String {
            return "OutpointFilter(${transactionHash.toReversedHex()}, $index)"
        }
    }
}
