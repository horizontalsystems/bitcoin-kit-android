package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.crypto.BloomFilter
import io.horizontalsystems.bitcoincore.extensions.toReversedByteArray
import io.horizontalsystems.bitcoincore.storage.FullOutputInfo
import io.horizontalsystems.bitcoincore.utils.Utils

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

        var transactionOutputs = storage.getMyOutputs()

        storage.lastBlock()?.height?.let { bestBlockHeight ->
            transactionOutputs = transactionOutputs.filter { needToSetToBloomFilter(it, bestBlockHeight) }
        }

        for (out in transactionOutputs) {
            val outpoint = out.output.transactionHashReversedHex.toReversedByteArray() + Utils.intToByteArray(out.output.index).reversedArray()
            elements.add(outpoint)
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
    private fun needToSetToBloomFilter(output: FullOutputInfo, bestBlockHeight: Int): Boolean {
        if (output.input == null) {
            return true
        }

        val outputSpentBlockHeight = output.input.block?.height
        if (outputSpentBlockHeight != null) {
            return bestBlockHeight - outputSpentBlockHeight < 100
        }

        return true
    }

}
