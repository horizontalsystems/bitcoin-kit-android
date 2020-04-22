package io.horizontalsystems.bitcoincore.crypto

import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.utils.Utils
import java.lang.Double.valueOf
import java.util.*
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow

/**
 * BloomFilter
 *
 *   Size       Field           Description
 *   ====       =====           ===========
 *   VarInt     Count           Number of bytes in the filter
 *   Variable   Filter          Filter data
 *   4 bytes    nHashFuncs      Number of hash functions
 *   4 bytes    nTweak          Random value to add to the hash seed
 *   1 byte     nFlags          Filter update flags
 */
class BloomFilter(elements: List<ByteArray>) {

    /** Filter data  */
    private val filter: ByteArray

    /** Number of hash functions  */
    private val nHashFuncs: Int

    /** Random tweak nonce  */
    private val nTweak = valueOf(Math.random() * Long.MAX_VALUE).toLong()

    /** Filter update flags  */
    private val nFlags = UPDATE_NONE

    init {
        val falsePositiveRate = 0.00005
        //
        // Allocate the filter array
        //
        val size = min((-1 / ln(2.0).pow(2.0) * elements.size.toDouble() * ln(falsePositiveRate)).toInt(),
                MAX_FILTER_SIZE * 8) / 8
        filter = ByteArray(if (size <= 0) 1 else size)
        //
        // Optimal number of hash functions for a given filter size and element count.
        //
        nHashFuncs = min((filter.size * 8 / elements.size.toDouble() * ln(2.0)).toInt(), MAX_HASH_FUNCS)

        elements.forEach {
            insert(it)
        }
    }

    /**
     * Inserts an bytes into the filter
     *
     * @param   bytes    Object to insert
     */
    private fun insert(bytes: ByteArray) {
        for (i in 0 until nHashFuncs) {
            Utils.setBitLE(filter, MurmurHash3.hash(filter, nTweak, i, bytes))
        }
    }

    /**
     * Serialize the filter
     */
    fun toByteArray(): ByteArray {
        val output = BitcoinOutput()
        output.writeVarInt(filter.size.toLong())
        output.write(filter)
        output.writeInt(nHashFuncs)
        output.writeUnsignedInt(nTweak)
        output.writeByte(nFlags)

        return output.toByteArray()
    }

    override fun toString(): String {
        return "Bloom Filter of size ${filter.size} with $nHashFuncs hash functions."
    }

    override fun equals(other: Any?) = when (other) {
        is BloomFilter -> filter.contentEquals(other.filter)
        else -> false
    }

    override fun hashCode(): Int {
        var result = filter.contentHashCode()
        result = 31 * result + nHashFuncs
        result = 31 * result + nTweak.hashCode()
        result = 31 * result + nFlags
        return result
    }

    companion object {

        /** Bloom filter - Filter is not adjusted for matching outputs  */
        const val UPDATE_NONE = 0

        /** Bloom filter - Filter is adjusted for all matching outputs  */
        const val UPDATE_ALL = 1

        /** Bloom filter - Filter is adjusted only for pay-to-pubkey or pay-to-multi-sig  */
        const val UPDATE_P2PUBKEY_ONLY = 2

        /** Maximum filter size  */
        const val MAX_FILTER_SIZE = 36000

        /** Maximum number of hash functions  */
        const val MAX_HASH_FUNCS = 50
    }
}
