package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.crypto.BloomFilter
import bitcoin.wallet.kit.hdwallet.PublicKey

class BloomFilterManager(elements: List<ByteArray>) {

    interface Listener {
        fun onFilterUpdated(bloomFilter: BloomFilter)
    }

    var listener: Listener? = null
    var bloomFilter: BloomFilter? = null
    private val elements = elements.toMutableList()

    init {
        if (elements.isNotEmpty()) {
            bloomFilter = BloomFilter(elements)
        }
    }

    fun add(keys: List<PublicKey>) {
        keys.forEach { key ->
            if (elements.none { it.contentEquals(key.publicKeyHash) }) {
                elements.add(key.publicKeyHash)
                elements.add(key.publicKey)
                elements.add(key.scriptHashP2WPKH)
            }
        }

        BloomFilter(elements).let {
            bloomFilter = it
            listener?.onFilterUpdated(it)
        }
    }

}
