package io.horizontalsystems.bitcoincore.blocks

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.Block

class BlockMedianTimeHelper(
    private val storage: IStorage,
    // This flag must be set ONLY when it's NOT possible to get needed blocks for median time calculation
    private val approximate: Boolean = false
) {
    private val medianTimeSpan = 11

    val medianTimePast: Long?
        get() =
            storage.lastBlock()?.let { lastBlock ->
                if (approximate) {
                    // The median time is 6 blocks earlier which is approximately equal to 1 hour.
                    lastBlock.timestamp - 3600
                } else {
                    medianTimePast(lastBlock)
                }
            }

    fun medianTimePast(block: Block): Long? {
        val startIndex = block.height - medianTimeSpan + 1
        val median = storage.timestamps(from = startIndex, to = block.height)

        return when {
            block.height >= medianTimeSpan && median.size < medianTimeSpan -> null
            else -> median[median.size / 2]
        }
    }

}
