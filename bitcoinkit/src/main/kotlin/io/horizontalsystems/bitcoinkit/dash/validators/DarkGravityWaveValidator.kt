package io.horizontalsystems.bitcoinkit.dash.validators

import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoincore.blocks.validators.IBlockValidator
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.crypto.CompactBits
import io.horizontalsystems.bitcoincore.models.Block
import java.math.BigInteger
import kotlin.math.min

class DarkGravityWaveValidator(
        private val storage: IStorage,
        private val heightInterval: Long,
        private val targetTimespan: Long,
        private val maxTargetBits: Long,
        private val firstCheckpoint: Int) : IBlockValidator {

    override fun validate(block: Block, previousBlock: Block) {
        check(previousBlock.height >= firstCheckpoint + heightInterval) {
            return
        }

        var actualTimeSpan = 0L
        var avgTargets = CompactBits.decode(previousBlock.bits)
        var prevBlock = previousBlock.previousBlock(storage)

        for (blockCount in 2..heightInterval) {
            val currentBlock = checkNotNull(prevBlock) {
                throw BlockValidatorException.NoPreviousBlock()
            }

            avgTargets *= BigInteger.valueOf(blockCount)
            avgTargets += CompactBits.decode(currentBlock.bits)
            avgTargets /= BigInteger.valueOf(blockCount + 1)

            if (blockCount < heightInterval) {
                prevBlock = currentBlock.previousBlock(storage)
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
        return true
    }
}
