package bitcoin.wallet.kit.blocks.validators

import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.walllet.kit.crypto.CompactBits

open class MainnetValidator(val network: NetworkParameters) : BlockValidator(network) {

    override fun validate(block: Block, previousBlock: Block) {
        validateHeader(block, previousBlock)

        if (isDifficultyTransitionEdge(block.height)) {
            checkDifficultyTransitions(block)
        } else {
            validateBits(block, previousBlock)
        }
    }

    open fun checkDifficultyTransitions(block: Block) {
        val lastCheckPointBlock = getLastCheckPointBlock(block)

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

    private fun getLastCheckPointBlock(block: Block): Block {
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
