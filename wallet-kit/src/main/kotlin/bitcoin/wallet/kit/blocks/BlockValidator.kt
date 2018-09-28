package bitcoin.wallet.kit.blocks

import bitcoin.wallet.kit.models.Block

object BlockValidator {

    fun validateHeader(block: Block, previousBlock: Block) {
        val blockHeader = checkNotNull(block.header) {
            throw BlockValidatorException.NoHeader()
        }

        check(blockHeader.prevHash.contentEquals(previousBlock.headerHash)) {
            throw BlockValidatorException.WrongPreviousHeader()
        }
    }

    fun validateBits(block: Block, previousBlock: Block) {
        val nextHeader = block.header
        val prevHeader = previousBlock.header

        if (prevHeader == null || nextHeader == null)
            throw BlockValidatorException.NoHeader()

        if (nextHeader.bits != prevHeader.bits)
            throw BlockValidatorException.NotEqualBits()
    }

    fun getLastCheckPointBlock(block: Block): Block {
        var lastCheckPointBlock = block

        for (i in 0 until 2016) {
            val prev = lastCheckPointBlock.previousBlock
            if (prev != null) {
                lastCheckPointBlock = prev
            } else throw when (i) {
                2015 -> BlockValidatorException.NoCheckpointBlock()
                else -> BlockValidatorException.NoPreviousBlock()
            }
        }

        return lastCheckPointBlock
    }
}

open class BlockValidatorException(msg: String) : RuntimeException(msg) {
    class NoHeader : BlockValidatorException("No Header")
    class NoCheckpointBlock : BlockValidatorException("No Checkpoint Block")
    class NoPreviousBlock : BlockValidatorException("No PreviousBlock")
    class WrongPreviousHeader : BlockValidatorException("Wrong Previous Header Hash")
    class NotEqualBits : BlockValidatorException("Not Equal Bits")
    class NotDifficultyTransitionEqualBits : BlockValidatorException("Not Difficulty Transition Equal Bits")
}
