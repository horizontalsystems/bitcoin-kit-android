package io.horizontalsystems.bitcoincore.blocks.validators

import io.horizontalsystems.bitcoincore.models.Block

interface IBlockValidator {
    fun validate(block: Block, previousBlock: Block)
    fun isBlockValidatable(block: Block, previousBlock: Block): Boolean
}
