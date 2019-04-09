package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.models.Block

class BlockHelper(private val storage: IStorage) {

    fun medianTimePast(block: Block): Long {
        val median = mutableListOf<Long>()
        var currentBlock = block

        for (i in 0 until 11) {
            median.add(currentBlock.timestamp)
            currentBlock = currentBlock.previousBlock(storage) ?: break
        }

        if (median.isEmpty()) {
            return currentBlock.timestamp
        }

        median.sort()
        return median[median.size / 2]
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
