package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.crypto.BloomFilter
import bitcoin.wallet.kit.hdwallet.PublicKey

class BloomFilterManager(elements: List<ByteArray>) {

    var bloomFilter: BloomFilter? = null
    private val elements = elements.toMutableList()

    private var updated = false

    init {
        if (elements.isNotEmpty()) {
            bloomFilter = BloomFilter(elements)
        }
    }

    fun getUpdatedBloomFilter(): BloomFilter? = when {
        updated -> {
            updated = false
            bloomFilter
        }
        else -> null
    }

    fun add(key: PublicKey) {
        elements.add(key.publicKeyHash)
        elements.add(key.publicKey)

        bloomFilter = BloomFilter(elements)
        updated = true
    }

}
