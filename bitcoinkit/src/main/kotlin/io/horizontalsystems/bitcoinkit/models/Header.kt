package io.horizontalsystems.bitcoinkit.models

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.utils.HashUtils
import java.io.IOException

/**
 * Header
 *
 *   Size       Field           Description
 *   ====       =====           ===========
 *   4 bytes    Version         The block version number
 *   32 bytes   PrevHash        The hash of the preceding block in the chain
 *   32 byte    MerkleHash      The Merkle root for the transactions in the block
 *   4 bytes    Time            The time the block was mined
 *   4 bytes    Bits            The target difficulty
 *   4 bytes    Nonce           The nonce used to generate the required hash
 */
open class Header {

    // Int32, block version information (note, this is signed)
    var version: Int = 0

    // The hash value of the previous block this particular block references
    var prevHash: ByteArray = byteArrayOf()

    // The reference to a Merkle tree collection which is a hash of all transactions related to this block
    var merkleHash: ByteArray = byteArrayOf()

    // Uint32, A timestamp recording when this block was created (Will overflow in 2106)
    var timestamp: Long = 0

    // Uint32, The calculated difficulty target being used for this block
    var bits: Long = 0

    // Uint32, The nonce used to generate this block to allow variations of the header and compute different hashes
    var nonce: Long = 0

    val hash: ByteArray by lazy {
        HashUtils.doubleSha256(toByteArray())
    }

    constructor()

    @Throws(IOException::class)
    constructor(input: BitcoinInput) {
        version = input.readInt()
        prevHash = input.readBytes(32)
        merkleHash = input.readBytes(32)
        timestamp = input.readUnsignedInt()
        bits = input.readUnsignedInt()
        nonce = input.readUnsignedInt()
    }

    fun toByteArray(): ByteArray {
        return BitcoinOutput()
                .writeInt(version)
                .write(prevHash)
                .write(merkleHash)
                .writeUnsignedInt(timestamp)
                .writeUnsignedInt(bits)
                .writeUnsignedInt(nonce)
                .toByteArray()
    }
}
