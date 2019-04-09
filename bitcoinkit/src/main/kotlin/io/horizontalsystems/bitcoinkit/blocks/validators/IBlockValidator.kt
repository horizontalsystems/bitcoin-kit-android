package io.horizontalsystems.bitcoinkit.blocks.validators

import io.horizontalsystems.bitcoinkit.models.Block

interface IBlockValidator {
    fun validate(block: Block, previousBlock: Block)
    fun isBlockValidatable(block: Block, previousBlock: Block): Boolean
}
