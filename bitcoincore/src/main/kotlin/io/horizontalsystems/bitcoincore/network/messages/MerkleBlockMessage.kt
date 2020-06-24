package io.horizontalsystems.bitcoincore.network.messages

import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.serializers.BlockHeaderParser
import io.horizontalsystems.bitcoincore.storage.BlockHeader

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
class MerkleBlockMessage(
        var header: BlockHeader,
        var txCount: Int,
        var hashCount: Int,
        var hashes: List<ByteArray>,
        var flagsCount: Int,
        var flags: ByteArray) : IMessage {

    private val blockHash: String by lazy {
        header.hash.toReversedHex()
    }

    override fun toString(): String {
        return "MerkleBlockMessage(blockHash=$blockHash, hashesSize=${hashes.size})"
    }
}

class MerkleBlockMessageParser(private val blockHeaderParser: BlockHeaderParser) : IMessageParser {
    override val command = "merkleblock"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
        val header = blockHeaderParser.parse(input)
        val txCount = input.readInt()

        val hashCount = input.readVarInt().toInt()
        val hashes: MutableList<ByteArray> = mutableListOf()
        repeat(hashCount) {
            hashes.add(input.readBytes(32))
        }

        val flagsCount = input.readVarInt().toInt()
        val flags = input.readBytes(flagsCount)

        return MerkleBlockMessage(header, txCount, hashCount, hashes, flagsCount, flags)
    }
}
