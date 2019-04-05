package io.horizontalsystems.bitcoinkit.blocks.validators

import io.horizontalsystems.bitcoinkit.models.Block

interface IBlockValidator {
    fun validate(block: Block, previousBlock: Block)
}

class BlockValidatorChain : IBlockValidator {
    private val concreteValidators = mutableListOf<IBlockValidator>()

    override fun validate(block: Block, previousBlock: Block) {
        concreteValidators.forEach {
            it.validate(block, previousBlock)
        }
    }

    fun addValidator(validator: IBlockValidator) {
        concreteValidators.add(validator)
    }
}
