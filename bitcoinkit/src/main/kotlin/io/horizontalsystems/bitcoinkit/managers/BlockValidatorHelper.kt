package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.models.Block

open class BlockValidatorHelper(protected val storage: IStorage) {

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
