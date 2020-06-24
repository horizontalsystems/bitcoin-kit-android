package io.horizontalsystems.bitcoincore.models

import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Reader

class Checkpoint(fileName: String) {
    val block: Block
    val additionalBlocks: List<Block>

    init {
        val stream = javaClass.classLoader?.getResourceAsStream(fileName)
        val inputStreamReader: Reader = InputStreamReader(stream)
        val reader = BufferedReader(inputStreamReader)
        val checkpoints = reader.readLines()

        val blocks = checkpoints.map { readBlock(it) }

        block = blocks.first()
        additionalBlocks = blocks.drop(1)
    }

    private fun readBlock(serializedCheckpointBlock: String): Block {
        BitcoinInputMarkable(serializedCheckpointBlock.hexToByteArray()).use { input ->
            val version = input.readInt()
            val prevHash = input.readBytes(32)
            val merkleHash = input.readBytes(32)
            val timestamp = input.readUnsignedInt()
            val bits = input.readUnsignedInt()
            val nonce = input.readUnsignedInt()
            val height = input.readInt()
            val hash = input.readBytes(32)

            return Block(BlockHeader(
                    version = version,
                    previousBlockHeaderHash = prevHash,
                    merkleRoot = merkleHash,
                    timestamp = timestamp,
                    bits = bits,
                    nonce = nonce,
                    hash = hash
            ), height)
        }
    }

}
