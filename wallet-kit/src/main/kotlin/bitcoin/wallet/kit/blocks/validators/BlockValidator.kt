package bitcoin.wallet.kit.blocks.validators

import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.network.NetworkParameters

abstract class BlockValidator(private val network: NetworkParameters) {

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

    fun isDifficultyTransitionEdge(height: Int): Boolean {
        return (height % network.heightInterval == 0L)
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
