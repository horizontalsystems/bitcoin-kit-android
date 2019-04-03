package io.horizontalsystems.bitcoinkit.blocks

import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.network.messages.MerkleBlockMessage
import io.horizontalsystems.bitcoinkit.utils.MerkleBranch

class MerkleBlockExtractor(private val maxBlockSize: Int) {

    fun extract(message: MerkleBlockMessage): MerkleBlock {
        val matchedHashes = mutableListOf<ByteArray>()
        val merkleRoot = MerkleBranch().calculateMerkleRoot(message.txCount, message.hashes, message.flags, matchedHashes)

        message.apply {
            if (txCount < 1 || txCount > maxBlockSize / 60) {
                throw InvalidMerkleBlockException(String.format("Transaction count %d is not valid", txCount))
            }

            if (hashCount < 0 || hashCount > txCount) {
                throw InvalidMerkleBlockException(String.format("Hash count %d is not valid", hashCount))
            }

            if (flagsCount < 1) {
                throw InvalidMerkleBlockException(String.format("Flag count %d is not valid", flagsCount))
            }

            if (!header.merkleRoot.contentEquals(merkleRoot)) {
                throw InvalidMerkleBlockException("Merkle root is not valid")
            }
        }

        return MerkleBlock(message.header, matchedHashes.map { it.toHexString() })
    }
}

class InvalidMerkleBlockException(message: String) : Exception(message)
