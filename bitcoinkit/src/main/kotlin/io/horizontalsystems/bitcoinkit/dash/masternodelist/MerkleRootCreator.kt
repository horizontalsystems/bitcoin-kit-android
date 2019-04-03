package io.horizontalsystems.bitcoinkit.dash.masternodelist

import io.horizontalsystems.bitcoinkit.dash.IMerkleHasher

class MerkleRootCreator(val hasher: IMerkleHasher) {

    fun create(hashes: List<ByteArray>): ByteArray? {
        if (hashes.isEmpty()) return null

        var tmpHashes = hashes

        do {
            tmpHashes = joinHashes(tmpHashes)
        } while (tmpHashes.size > 1)

        return tmpHashes.first()
    }

    private fun joinHashes(hashes: List<ByteArray>): List<ByteArray> {
        val chunks = hashes.chunked(2)

        return chunks.map {
            hasher.hash(it.first(), it.last())
        }
    }
}
