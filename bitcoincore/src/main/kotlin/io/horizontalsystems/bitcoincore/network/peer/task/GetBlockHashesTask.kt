package io.horizontalsystems.bitcoincore.network.peer.task

import io.horizontalsystems.bitcoincore.models.InventoryItem
import io.horizontalsystems.bitcoincore.network.messages.GetBlocksMessage
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.messages.InvMessage
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class GetBlockHashesTask(private val blockLocatorHashes: List<ByteArray>, expectedHashesMinCount: Int) : PeerTask() {

    var blockHashes = listOf<ByteArray>()

    private val maxAllowedIdleTime = TimeUnit.SECONDS.toMillis(10)
    private val minAllowedIdleTime = TimeUnit.SECONDS.toMillis(1)
    private val maxExpectedBlockHashesCount: Int = 500
    private val minExpectedBlockHashesCount: Int = 6

    private val expectedHashesMinCount: Int

    init {
        this.expectedHashesMinCount = min(max(minExpectedBlockHashesCount, expectedHashesMinCount), maxExpectedBlockHashesCount)

        val resolvedAllowedIdleTime = maxAllowedIdleTime * this.expectedHashesMinCount / maxExpectedBlockHashesCount.toDouble()
        allowedIdleTime = max(minAllowedIdleTime, resolvedAllowedIdleTime.toLong())
    }

    override val state: String
        get() = "expectedHashesMinCount: $expectedHashesMinCount; allowedIdleTime: $allowedIdleTime"

    override fun start() {
        requester?.let { it.send(GetBlocksMessage(blockLocatorHashes, it.protocolVersion, ByteArray(32))) }
        resetTimer()
    }

    override fun handleMessage(message: IMessage): Boolean {
        if (message !is InvMessage) {
            return false
        }

        val newBlockHashes = message.inventory.filter { it.type == InventoryItem.MSG_BLOCK }.map { it.hash }
        if (newBlockHashes.isEmpty()) return false

        // When we send getblocks message the remote peer responds with 2 inv messages:
        //  - one of them is the message we are awaiting
        //  - another is the last block in the peer
        // Based on bitcoin protocol it should respond with only one inv message.
        // It can send the second inv message only if it has a stale block.
        // That is why we take the inv message with the last block as the stale block.
        // When we in the last iteration of syncing we send the last block in the block locator hashes.
        // And if the remote peer sends us the inv message with the last block we should ignore it.
        newBlockHashes.forEach { newBlockHash ->
            blockLocatorHashes.forEach { blockHash ->
                if (blockHash.contentEquals(newBlockHash)) {
                    return true
                }
            }
        }

        if (newBlockHashes.size > blockHashes.size) {
            blockHashes = newBlockHashes
        }

        if (newBlockHashes.size >= expectedHashesMinCount) {
            listener?.onTaskCompleted(this)
        }

        return true
    }

    override fun handleTimeout() {
        listener?.onTaskCompleted(this)
    }
}
