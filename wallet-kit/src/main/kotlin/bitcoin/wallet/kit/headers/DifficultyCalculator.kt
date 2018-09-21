package bitcoin.wallet.kit.headers

import bitcoin.wallet.kit.models.Block
import bitcoin.walllet.kit.utils.Utils

class DifficultyCalculator {
    private val maxTargetBits = Utils.decodeCompactBits(0x1d00ffffL)    // Maximum difficulty

    private val targetTimespan: Long = 14 * 24 * 60 * 60                        // 2 weeks per difficulty cycle, on average.
    private val targetSpacing = 10 * 60                                         // 10 minutes per block.

    var heightInterval = targetTimespan / targetSpacing                         // 2016 blocks

    fun difficultyAfter(previousBlock: Block, lastCheckPointBlock: Block): Long {
        val blockHeader = previousBlock.header
        val lastCheckPointBlockHeader = lastCheckPointBlock.header

        if (blockHeader == null || lastCheckPointBlockHeader == null) {
            throw Exception("NoHeader")
        }

        // Limit the adjustment step.
        var timespan = blockHeader.timestamp - lastCheckPointBlockHeader.timestamp
        if (timespan < targetTimespan / 4)
            timespan = targetTimespan / 4
        if (timespan > targetTimespan * 4)
            timespan = targetTimespan * 4

        var newTarget = Utils.decodeCompactBits(blockHeader.bits)
        newTarget = newTarget.multiply(timespan.toBigInteger())
        newTarget = newTarget.divide(targetTimespan.toBigInteger())

        // Difficulty hit proof of work limit: newTarget.toString(16)
        if (newTarget > maxTargetBits) {
            newTarget = maxTargetBits
        }

        return Utils.encodeCompactBits(newTarget)
    }
}
