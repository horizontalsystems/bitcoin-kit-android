package io.horizontalsystems.bitcoinkit.messages

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.utils.HashUtils
import java.io.ByteArrayInputStream

/**
 * MerkleBlock Message
 *
 *  Size        Field           Description
 *  ====        =====           ===========
 *  80 bytes    Header          Consists of 6 fields that are hashed to calculate the block hash
 *  VarInt      hashCount       Number of hashes
 *  Variable    hashes          Hashes in depth-first order
 *  VarInt      flagsCount      Number of bytes of flag bits
 *  Variable    flagsBits       Flag bits packed 8 per byte, least significant bit first
 */
class MerkleBlockMessage() : Message("merkleblock") {

    lateinit var merkleBlock: MerkleBlock

    constructor(payload: ByteArray) : this() {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            merkleBlock = MerkleBlock(input)
        }
    }

    override fun getPayload(): ByteArray {
        return byteArrayOf()
    }

    override fun toString(): String {
        return "MerkleBlockMessage(blockHash=${HashUtils.toHexStringAsLE(merkleBlock.blockHash)}, hashesSize=${merkleBlock.hashes.size})"
    }
}
