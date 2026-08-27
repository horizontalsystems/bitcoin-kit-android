package io.horizontalsystems.bitcoincore.apisync

import io.horizontalsystems.bitcoincore.apisync.blockchair.BlockchairBlockHashFetcher
import io.horizontalsystems.bitcoincore.apisync.blockchair.IBlockHashFetcher

class BlockHashFetcher(
    private val hsBlockHashFetcher: HsBlockHashFetcher,
    private val blockchairBlockHashFetcher: BlockchairBlockHashFetcher,
    private val checkpointHeight: Int
) : IBlockHashFetcher {

    override fun fetch(heights: List<Int>): Map<Int, String> {
        val beforeCheckpoint = heights.filter { it <= checkpointHeight }
        val afterCheckpoint = heights.filter { it > checkpointHeight }

        val blockHashes = mutableMapOf<Int, String>()
        if (beforeCheckpoint.isNotEmpty()) {
            blockHashes += hsBlockHashFetcher.fetch(beforeCheckpoint)
        }

        // The HS index can lag behind the checkpoint or have holes in its backfill; any
        // height it did not return is resolved through Blockchair rather than failing the sync.
        val remaining = afterCheckpoint + beforeCheckpoint.filter { it !in blockHashes }
        if (remaining.isNotEmpty()) {
            blockHashes += blockchairBlockHashFetcher.fetch(remaining)
        }

        return blockHashes
    }

}
