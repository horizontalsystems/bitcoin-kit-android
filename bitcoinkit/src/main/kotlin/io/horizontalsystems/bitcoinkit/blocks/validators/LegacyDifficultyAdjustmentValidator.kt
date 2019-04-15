package io.horizontalsystems.bitcoinkit.blocks.validators

import io.horizontalsystems.bitcoinkit.crypto.CompactBits
import io.horizontalsystems.bitcoinkit.managers.BlockValidatorHelper
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.network.Network
import java.math.BigInteger
import kotlin.math.min

class LegacyDifficultyAdjustmentValidator(network: Network, private val blockValidatorHelper: BlockValidatorHelper) : IBlockValidator {
    private val heightInterval = network.heightInterval
    private val targetTimespan = network.targetTimespan
    private val maxTargetBits = network.maxTargetBits

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return block.height % heightInterval == 0L
    }

    override fun validate(block: Block, previousBlock: Block) {
        val lastCheckPointBlock = checkNotNull(blockValidatorHelper.getPrevious(block, 2016)) {
            BlockValidatorException.NoCheckpointBlock()
        }

        //  Limit the adjustment step
        var timespan = previousBlock.timestamp - lastCheckPointBlock.timestamp
        if (timespan < targetTimespan / 4)
            timespan = targetTimespan / 4
        if (timespan > targetTimespan * 4)
            timespan = targetTimespan * 4

        var newTarget = CompactBits.decode(previousBlock.bits)
        newTarget = newTarget.multiply(BigInteger.valueOf(timespan))
        newTarget = newTarget.divide(BigInteger.valueOf(targetTimespan))

        //  Difficulty hit proof of work limit: newTarget.toString(16)
        val newDifficulty = min(CompactBits.encode(newTarget), maxTargetBits)

        if (newDifficulty != block.bits) {
            throw BlockValidatorException.NotDifficultyTransitionEqualBits()
        }
    }
}
