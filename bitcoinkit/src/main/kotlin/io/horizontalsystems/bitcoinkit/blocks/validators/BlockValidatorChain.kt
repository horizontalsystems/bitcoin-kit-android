package io.horizontalsystems.bitcoinkit.blocks.validators

import io.horizontalsystems.bitcoinkit.models.Block

class BlockValidatorChain(private val proofOfWorkValidator: BlockHeaderValidator) : IBlockValidator {

    private val concreteValidators = mutableListOf<IBlockValidator>()

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return true
    }

    override fun validate(block: Block, previousBlock: Block) {
        proofOfWorkValidator.validate(block, previousBlock)

        concreteValidators.firstOrNull { validator ->
            validator.isBlockValidatable(block, previousBlock)
        }?.validate(block, previousBlock)
    }

    fun add(validator: IBlockValidator) {
        concreteValidators.add(validator)
    }
}
