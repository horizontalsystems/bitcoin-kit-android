package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.crypto.BloomFilter
import io.horizontalsystems.bitcoincore.utils.Utils

class BloomFilterManager(private val storage: IStorage) {

    object BloomFilterExpired : Exception()

    interface Listener {
        fun onFilterUpdated(bloomFilter: BloomFilter)
    }

    var listener: Listener? = null
    var bloomFilter: BloomFilter? = null
    private var bloomFilterProviders = mutableListOf<IBloomFilterProvider>()

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

        val transactionOutputs = storage.lastBlock()?.height?.let { lastBlockHeight ->
            // get transaction outputs which are unspent or spent in last 100 blocks
            storage.getOutputsForBloomFilter(lastBlockHeight - 100)
        } ?: listOf()

        for (output in transactionOutputs) {
            val outpoint = output.transactionHash + Utils.intToByteArray(output.index).reversedArray()
            elements.add(outpoint)
        }

        bloomFilterProviders.forEach {
            elements.addAll(it.getBloomFilterElements())
        }

        if (elements.isNotEmpty()) {
            BloomFilter(elements).let {
                bloomFilter = it
                listener?.onFilterUpdated(it)
            }
        }
    }

    fun addBloomFilterProvider(provider: IBloomFilterProvider) {
        provider.bloomFilterManager = this
        bloomFilterProviders.add(provider)
    }
}

interface IBloomFilterProvider {
    var bloomFilterManager: BloomFilterManager?

    fun getBloomFilterElements(): List<ByteArray>
}
