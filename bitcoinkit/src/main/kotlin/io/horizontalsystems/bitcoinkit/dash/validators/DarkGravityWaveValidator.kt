package io.horizontalsystems.bitcoinkit.dash.validators

import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoincore.blocks.validators.IBlockValidator
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.crypto.CompactBits
import io.horizontalsystems.bitcoincore.models.Block
import java.math.BigInteger

class DarkGravityWaveValidator(
        private val storage: IStorage,
        private val heightInterval: Long,
        private val targetTimespan: Long,
        private val maxTargetBits: BigInteger) : IBlockValidator {

    override fun validate(candidate: Block, block: Block) {
        var actualTimeSpan = 0L
        var avgTargets = CompactBits.decode(block.bits)
        var prevBlock = block.previousBlock(storage)

        for (blockCount in 2..heightInterval) {
            val currentBlock = checkNotNull(prevBlock) { throw BlockValidatorException.NoPreviousBlock() }

            val currentTarget = CompactBits.decode(currentBlock.bits)
            avgTargets = (avgTargets * BigInteger.valueOf(blockCount) + currentTarget) / BigInteger.valueOf(blockCount + 1)

            if (blockCount < heightInterval) {
                prevBlock = currentBlock.previousBlock(storage)
            } else {
                actualTimeSpan = block.timestamp - currentBlock.timestamp
            }
        }

        var darkTarget = avgTargets

        if (actualTimeSpan < targetTimespan / 3)
            actualTimeSpan = targetTimespan / 3
        if (actualTimeSpan > targetTimespan * 3)
            actualTimeSpan = targetTimespan * 3

        //  Retarget
        darkTarget = darkTarget * BigInteger.valueOf(actualTimeSpan) / BigInteger.valueOf(targetTimespan)

        if (darkTarget > maxTargetBits) {
            darkTarget = maxTargetBits
        }

        val compact = CompactBits.encode(darkTarget)
        if (compact != candidate.bits) {
            throw BlockValidatorException.NotEqualBits()
        }
    }

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return true
    }
}
