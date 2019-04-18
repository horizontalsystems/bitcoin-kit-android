package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.Block

open class BlockValidatorHelper(protected val storage: IStorage) {
    fun getPreviousChunk(blockHeight: Int, size: Int): List<Block> {
        return storage.getBlocksChunk(fromHeight = blockHeight - size, toHeight = blockHeight)
    }

    fun getPrevious(block: Block, stepBack: Int): Block? {
        return getPreviousWindow(block, stepBack)?.firstOrNull()
    }

    fun getPreviousWindow(block: Block, size: Int): Array<Block>? {
        val blocks = mutableListOf<Block>()
        var prev = block
        for (i in 0 until size) {
            prev = prev.previousBlock(storage) ?: return null
            blocks.add(0, prev)
        }

        return blocks.toTypedArray()
    }
}
