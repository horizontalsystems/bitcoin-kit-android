package bitcoin.wallet.kit.headers

import bitcoin.wallet.kit.models.Block

class BlockValidator {
    private val calculator = DifficultyCalculator()

    enum class ValidatorError {
        NoCheckpointBlock,
        NoPreviousBlock,
        WrongPreviousHeaderHash,
        NotEqualBits,
        NotDifficultyTransitionEqualBits
    }

    class InvalidBlock(error: ValidatorError) : Exception(error.toString())

    fun validate(block: Block) {
        validateHash(block)

        if (isDifficultyTransitionPoint(block.height)) {
            validateDifficultyTransition(block)
        } else if (block.header?.bits != block.previousBlock?.header?.bits) {
            throw InvalidBlock(ValidatorError.NotEqualBits)
        }
    }

    private fun validateHash(block: Block) {
        val previousBlock = block.previousBlock
        if (previousBlock == null) {
            throw InvalidBlock(ValidatorError.NoPreviousBlock)
        }

        val blockHeader = block.header
        if (blockHeader == null || blockHeader.prevHash != previousBlock.headerHash) {
            throw InvalidBlock(ValidatorError.WrongPreviousHeaderHash)
        }
    }

    private fun isDifficultyTransitionPoint(height: Int): Boolean {
        return (height % calculator.heightInterval == 0L)
    }

    private fun validateDifficultyTransition(block: Block) {
        var lastCheckPointBlock = block

        for (i in 0..2015) {
            val tmpBlock = lastCheckPointBlock.previousBlock
            if (tmpBlock != null) {
                lastCheckPointBlock = tmpBlock
            } else throw when (i) {
                2015 -> InvalidBlock(ValidatorError.NoCheckpointBlock)
                else -> InvalidBlock(ValidatorError.NoPreviousBlock)
            }
        }

        val previousBlock = block.previousBlock
        if (previousBlock == null) {
            throw InvalidBlock(ValidatorError.NoPreviousBlock)
        }

        if (calculator.difficultyAfter(previousBlock, lastCheckPointBlock) != block.header?.bits) {
            throw InvalidBlock(ValidatorError.NotDifficultyTransitionEqualBits)
        }
    }
}
