package io.horizontalsystems.bitcoinkit.blocks.validators

import io.horizontalsystems.bitcoinkit.crypto.CompactBits
import io.horizontalsystems.bitcoinkit.managers.BlockHelper
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.network.Network

class LegacyDifficultyAdjustmentValidator(private val network: Network, private val blockHelper: BlockHelper) : IBlockValidator {

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return block.height % network.heightInterval == 0L
    }

    override fun validate(block: Block, previousBlock: Block) {
        val lastCheckPointBlock = checkNotNull(blockHelper.getPrevious(block, 2016)) {
            BlockValidatorException.NoCheckpointBlock()
        }

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
}
