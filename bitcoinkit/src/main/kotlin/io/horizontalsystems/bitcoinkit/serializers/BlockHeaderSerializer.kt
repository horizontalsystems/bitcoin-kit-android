package io.horizontalsystems.bitcoinkit.serializers

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.storage.BlockHeader
import io.horizontalsystems.bitcoinkit.utils.HashUtils

object BlockHeaderSerializer {

    fun deserialize(input: BitcoinInput): BlockHeader {
        val version = input.readInt()
        val previousBlockHeaderHash = input.readBytes(32)
        val merkleRoot = input.readBytes(32)
        val timestamp = input.readUnsignedInt()
        val bits = input.readUnsignedInt()
        val nonce = input.readUnsignedInt()

        val payload = serialize(version, previousBlockHeaderHash, merkleRoot, timestamp, bits, nonce)

        val hash = HashUtils.doubleSha256(payload)

        return BlockHeader(version, previousBlockHeaderHash, merkleRoot, timestamp, bits, nonce, hash)
    }

    private fun serialize(version: Int, previousBlockHeaderHash: ByteArray, merkleRoot: ByteArray, timestamp: Long, bits: Long, nonce: Long): ByteArray {
        return BitcoinOutput()
                .writeInt(version)
                .write(previousBlockHeaderHash)
                .write(merkleRoot)
                .writeUnsignedInt(timestamp)
                .writeUnsignedInt(bits)
                .writeUnsignedInt(nonce)
                .toByteArray()
    }
}
