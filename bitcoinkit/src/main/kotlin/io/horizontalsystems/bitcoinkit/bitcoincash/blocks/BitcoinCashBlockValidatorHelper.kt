package io.horizontalsystems.bitcoinkit.bitcoincash.blocks

import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.managers.BlockValidatorHelper
import io.horizontalsystems.bitcoinkit.models.Block

class BitcoinCashBlockValidatorHelper(storage: IStorage) : BlockValidatorHelper(storage) {

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

    private fun MutableList<Block>.swap(index1: Int, index2: Int) {
        val tmp = this[index1]
        this[index1] = this[index2]
        this[index2] = tmp
    }

    //  Get median of last 3 blocks based on timestamp
    fun getSuitableBlock(block: Block): Block {
        val blocks: MutableList<Block> = mutableListOf()
        blocks.add(block)
        blocks.add(blocks[0].previousBlock(storage) ?: throw BlockValidatorException.NoPreviousBlock())
        blocks.add(blocks[1].previousBlock(storage) ?: throw BlockValidatorException.NoPreviousBlock())

        blocks.reverse()

        if (blocks[0].timestamp > blocks[2].timestamp) {
            blocks.swap(0, 2)
        }

        if (blocks[0].timestamp > blocks[1].timestamp) {
            blocks.swap(0, 1)
        }

        if (blocks[1].timestamp > blocks[2].timestamp) {
            blocks.swap(1, 2)
        }

        return blocks[1]
    }
}
