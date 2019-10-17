package io.horizontalsystems.bitcoincore.blocks

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.Block

class BlockMedianTimeHelper(private val storage: IStorage) {
    private val medianTimeSpan = 11

    val medianTimePast: Long?
        get() = storage.lastBlock()?.let { medianTimePast(it) }

    fun medianTimePast(block: Block): Long? {
        val startIndex = block.height - medianTimeSpan + 1
        val median = storage.timestamps(from = startIndex, to = block.height)

        return when {
            block.height >= medianTimeSpan && median.size < medianTimeSpan -> null
            else -> median[median.size / 2]
        }
    }

    // Returns (an approximate medianTimePast of a block in which given transaction is included) PLUS ~1 hour.
    // This is not an accurate medianTimePast, it always returns a timestamp nearly 7 blocks ahead.
    // But this is quite enough in our case since we're setting relative time-locks for at least 1 month
    fun medianTimePast(transactionHash: ByteArray): Long? {
        return storage.getTransaction(transactionHash)?.timestamp
    }

}
