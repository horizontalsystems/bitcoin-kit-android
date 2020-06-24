package io.horizontalsystems.bitcoincore.models

import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import java.io.IOException

/**
 * InventoryItem
 *
 *   Size       Field   Description
 *   ====       =====   ===========
 *   4 bytes    Type    0=Error, 1=Transaction, 2=Block, 3=Filtered Block
 *  32 bytes    Hash    Object hash
 */
class InventoryItem {

    // Uint32
    var type: Int = 0

    // 32-bytes hash
    lateinit var hash: ByteArray

    constructor()

    @Throws(IOException::class)
    constructor(input: BitcoinInputMarkable) {
        this.type = input.readInt()
        this.hash = input.readBytes(32)
    }

    constructor(type: Int, hash: ByteArray) {
        this.type = type
        this.hash = hash
    }

    fun toByteArray(): ByteArray {
        return BitcoinOutput().writeInt(this.type).write(this.hash).toByteArray()
    }

    companion object {

        /**
         * Any data of with this number may be ignored.
         */
        const val ERROR = 0

        /**
         * Hash is related to a transaction.
         */
        const val MSG_TX = 1

        /**
         * Hash is related to a data block.
         */
        const val MSG_BLOCK = 2

        /**
         * Hash of a block header; identical to MSG_BLOCK. Only to be used in
         * getdata message. Indicates the reply should be a merkleblock message
         * rather than a block message; this only works if a bloom filter has been
         * set.
         */
        const val MSG_FILTERED_BLOCK = 3

        /**
         * Hash of a block header; identical to MSG_BLOCK. Only to be used in
         * getdata message. Indicates the reply should be a cmpctblock message. See
         * BIP 152 for more info.
         */
        const val MSG_CMPCT_BLOCK = 4
    }
}
