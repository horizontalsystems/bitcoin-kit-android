package io.horizontalsystems.bitcoinkit.serializers

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.storage.BlockHeader

object BlockHeaderSerializer {
    fun serialize(header: BlockHeader): ByteArray {
        return BitcoinOutput()
                .writeInt(header.version)
                .write(header.previousBlockHeaderHash)
                .write(header.merkleRoot)
                .writeUnsignedInt(header.timestamp)
                .writeUnsignedInt(header.bits)
                .writeUnsignedInt(header.nonce)
                .toByteArray()
    }

    fun deserialize(input: BitcoinInput): BlockHeader {
        val version = input.readInt()
        val prevHash = input.readBytes(32)
        val merkleHash = input.readBytes(32)
        val timestamp = input.readUnsignedInt()
        val bits = input.readUnsignedInt()
        val nonce = input.readUnsignedInt()

        return BlockHeader(version, prevHash, merkleHash, timestamp, bits, nonce)
    }
}
