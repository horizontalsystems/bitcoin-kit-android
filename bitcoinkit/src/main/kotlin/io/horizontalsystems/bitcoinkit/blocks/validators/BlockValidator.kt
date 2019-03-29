package io.horizontalsystems.bitcoinkit.blocks.validators

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.crypto.CompactBits
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.network.Network
import java.math.BigInteger

open class BlockValidator(private val network: Network, private val storage: IStorage) {

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

        val previousBlock = block.previousBlock(storage) ?: throw BlockValidatorException.NoPreviousBlock()

        //  Limit the adjustment step
        var timespan = previousBlock.timestamp - lastCheckPointBlock.timestamp
        if (timespan < network.targetTimespan / 4)
            timespan = network.targetTimespan / 4
        if (timespan > network.targetTimespan * 4)
            timespan = network.targetTimespan * 4

        var newTarget = CompactBits.decode(previousBlock.bits)
        newTarget = newTarget.multiply(timespan.toBigInteger())
        newTarget = newTarget.divide(network.targetTimespan.toBigInteger())

        // Difficulty hit proof of work limit: newTarget.toString(16)
        if (newTarget > network.maxTargetBits) {
            newTarget = network.maxTargetBits
        }

        val newTargetCompact = CompactBits.encode(newTarget)
        if (newTargetCompact != block.bits) {
            throw BlockValidatorException.NotDifficultyTransitionEqualBits()
        }
    }

    fun validateHeader(block: Block, previousBlock: Block) {
        check(BigInteger(block.headerHashReversedHex, 16) < CompactBits.decode(block.bits)) {
            throw BlockValidatorException.InvalidProveOfWork()
        }

        check(block.previousBlockHash.contentEquals(previousBlock.headerHash)) {
            throw BlockValidatorException.WrongPreviousHeader()
        }
    }

    fun validateBits(block: Block, previousBlock: Block) {
        if (block.bits != previousBlock.bits)
            throw BlockValidatorException.NotEqualBits()
    }

    fun isDifficultyTransitionEdge(height: Int): Boolean {
        return (height % network.heightInterval == 0L)
    }

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

open class BlockValidatorException(msg: String) : RuntimeException(msg) {
    class NoHeader : BlockValidatorException("No Header")
    class NoCheckpointBlock : BlockValidatorException("No Checkpoint Block")
    class NoPreviousBlock : BlockValidatorException("No PreviousBlock")
    class WrongPreviousHeader : BlockValidatorException("Wrong Previous Header Hash")
    class NotEqualBits : BlockValidatorException("Not Equal Bits")
    class NotDifficultyTransitionEqualBits : BlockValidatorException("Not Difficulty Transition Equal Bits")
    class InvalidProveOfWork : BlockValidatorException("Invalid Prove of Work")
}
