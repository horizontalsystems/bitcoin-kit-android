package bitcoin.wallet.kit.blocks.validators

import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.walllet.kit.crypto.CompactBits

open class BlockValidator(private val network: NetworkParameters) {

    open fun validate(candidate: Block, previousBlock: Block) {
        validateHeader(candidate, previousBlock)

        if (isDifficultyTransitionEdge(candidate.height)) {
            checkDifficultyTransitions(candidate)
        } else {
            validateBits(candidate, previousBlock)
        }
    }

    open fun checkDifficultyTransitions(block: Block) {
        val lastCheckPointBlock = checkNotNull(getPrevious(block, 2016)) {
            BlockValidatorException.NoCheckpointBlock()
        }

        val previousBlock = checkNotNull(block.previousBlock) {
            throw BlockValidatorException.NoPreviousBlock()
        }

        val blockHeader = previousBlock.header
        val lastCheckPointBlockHeader = lastCheckPointBlock.header

        if (blockHeader == null || lastCheckPointBlockHeader == null) {
            throw BlockValidatorException.NoHeader()
        }

        // Limit the adjustment step
        var timespan = blockHeader.timestamp - lastCheckPointBlockHeader.timestamp
        if (timespan < network.targetTimespan / 4)
            timespan = network.targetTimespan / 4
        if (timespan > network.targetTimespan * 4)
            timespan = network.targetTimespan * 4

        var newTarget = CompactBits.decode(blockHeader.bits)
        newTarget = newTarget.multiply(timespan.toBigInteger())
        newTarget = newTarget.divide(network.targetTimespan.toBigInteger())

        // Difficulty hit proof of work limit: newTarget.toString(16)
        if (newTarget > network.maxTargetBits) {
            newTarget = network.maxTargetBits
        }

        val newTargetCompact = CompactBits.encode(newTarget)
        if (newTargetCompact != block.header?.bits) {
            throw BlockValidatorException.NotDifficultyTransitionEqualBits()
        }
    }

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

    fun getPrevious(block: Block, stepBack: Int): Block? {
        return getPreviousWindow(block, stepBack)?.first()
    }

    fun getPreviousWindow(block: Block, size: Int): Array<Block>? {
        val blocks = mutableListOf<Block>()
        var prev = block
        for (i in 0 until size) {
            prev = prev.previousBlock ?: return null
            blocks.add(0, prev)
        }

        return blocks.toTypedArray()
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
