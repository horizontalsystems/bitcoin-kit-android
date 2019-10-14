package io.horizontalsystems.bitcoincash.blocks

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.managers.BlockValidatorHelper
import io.horizontalsystems.bitcoincore.models.Block

class BitcoinCashBlockValidatorHelper(storage: IStorage) : BlockValidatorHelper(storage) {

    //  Get median of last 3 blocks based on timestamp
    fun getSuitableBlock(blocks: MutableList<Block>): Block {

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

    private fun MutableList<Block>.swap(index1: Int, index2: Int) {
        val tmp = this[index1]
        this[index1] = this[index2]
        this[index2] = tmp
    }
}
