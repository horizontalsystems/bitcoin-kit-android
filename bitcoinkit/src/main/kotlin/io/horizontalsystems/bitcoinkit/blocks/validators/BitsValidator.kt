package io.horizontalsystems.bitcoinkit.blocks.validators

import io.horizontalsystems.bitcoinkit.models.Block

class BitsValidator : IBlockValidator {

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return true
    }

    override fun validate(block: Block, previousBlock: Block) {
        if (block.bits != previousBlock.bits) {
            throw BlockValidatorException.NotEqualBits()
        }
    }

}
