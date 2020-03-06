package io.horizontalsystems.dashkit.validators

import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoincore.blocks.validators.IBlockChainedValidator
import io.horizontalsystems.bitcoincore.crypto.CompactBits
import io.horizontalsystems.bitcoincore.managers.BlockValidatorHelper
import io.horizontalsystems.bitcoincore.models.Block
import java.math.BigInteger
import kotlin.math.min

class DarkGravityWaveValidator(
        private val blockHelper: BlockValidatorHelper,
        private val heightInterval: Long,
        private val targetTimespan: Long,
        private val maxTargetBits: Long,
        private val powDGWHeight: Int
) : IBlockChainedValidator {

    override fun validate(block: Block, previousBlock: Block) {
        var actualTimeSpan = 0L
        var avgTargets = CompactBits.decode(previousBlock.bits)
        var prevBlock = blockHelper.getPrevious(previousBlock, 1)

        for (blockCount in 2..heightInterval) {
            val currentBlock = checkNotNull(prevBlock) {
                throw BlockValidatorException.NoPreviousBlock()
            }

            avgTargets *= BigInteger.valueOf(blockCount)
            avgTargets += CompactBits.decode(currentBlock.bits)
            avgTargets /= BigInteger.valueOf(blockCount + 1)

            if (blockCount < heightInterval) {
                prevBlock = blockHelper.getPrevious(currentBlock, 1)
            } else {
                actualTimeSpan = previousBlock.timestamp - currentBlock.timestamp
            }
        }

        var darkTarget = avgTargets

        if (actualTimeSpan < targetTimespan / 3)
            actualTimeSpan = targetTimespan / 3
        if (actualTimeSpan > targetTimespan * 3)
            actualTimeSpan = targetTimespan * 3

        //  Retarget
        darkTarget = darkTarget * BigInteger.valueOf(actualTimeSpan) / BigInteger.valueOf(targetTimespan)

        val compact = min(CompactBits.encode(darkTarget), maxTargetBits)
        if (compact != block.bits) {
            throw BlockValidatorException.NotEqualBits()
        }
    }

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return block.height >= powDGWHeight
    }
}
