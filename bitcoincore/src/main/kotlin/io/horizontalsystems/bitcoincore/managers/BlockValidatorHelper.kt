package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.Block

open class BlockValidatorHelper(protected val storage: IStorage) {
    fun getPreviousChunk(blockHeight: Int, size: Int): List<Block> {
        return storage.getBlocksChunk(fromHeight = blockHeight - size, toHeight = blockHeight)
    }

    /**
     * NOTE: When there is a fork there may be 2 blocks with the same height.
     *
     * In this case we need to retrieve block that is related to the current syncing peer.
     * The blocks of current syncing peer are stored in a database with "stale" set to true.
     * So if there is a 2 blocks with the same height we need to retrieve one with "stale" = true.
     *
     * Prioritizing stale blocks resolves it:
     *  - if there are 2 blocks then it will retrieve one with "stale" = true
     *  - if there is only 1 block then it will retrieve it. No matter what is set for "stale"
     *  - if there is no block for the given height then it will return null
     */
    fun getPrevious(block: Block, stepBack: Int): Block? {
        return storage.getBlockByHeightStalePrioritized(block.height - stepBack)
    }
}
