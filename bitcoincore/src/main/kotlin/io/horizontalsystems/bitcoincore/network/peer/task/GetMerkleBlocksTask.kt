package io.horizontalsystems.bitcoincore.network.peer.task

import io.horizontalsystems.bitcoincore.blocks.MerkleBlockExtractor
import io.horizontalsystems.bitcoincore.core.HashBytes
import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.models.InventoryItem
import io.horizontalsystems.bitcoincore.models.MerkleBlock
import io.horizontalsystems.bitcoincore.network.messages.GetDataMessage
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.messages.MerkleBlockMessage
import io.horizontalsystems.bitcoincore.network.messages.TransactionMessage
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import kotlin.math.roundToInt

class GetMerkleBlocksTask(
        hashes: List<BlockHash>,
        private val merkleBlockHandler: MerkleBlockHandler,
        private val merkleBlockExtractor: MerkleBlockExtractor,
        private val minMerkleBlocks: Double,
        private val minTransactions: Double,
        private val minReceiveBytes: Double) : PeerTask() {

    interface MerkleBlockHandler {
        fun handleMerkleBlock(merkleBlock: MerkleBlock)
    }

    private var blockHashes = hashes.toMutableList()
    private var pendingMerkleBlocks = mutableListOf<MerkleBlock>()

    // Following fields used to calculate peer speed
    private var receivedMerkleBlocks = 0
    private var receivedTransactions = 0
    private var receivedBytes = 0

    private var totalWaitingTime: Long = 0
    private var waitingStartTime: Long = 0
    private var maxWarningCount = 10
    private var firstResponseReceived = false

    override val state: String
        get() = "minMerkleBlocksCount: ${minMerkleBlocks.roundToInt()}; minTransactionsCount: ${minTransactions.roundToInt()}; minTransactionsSize: ${minTransactions.roundToInt()}"

    override fun start() {
        val items = blockHashes.map { hash ->
            InventoryItem(InventoryItem.MSG_FILTERED_BLOCK, hash.headerHash)
        }

        requester?.send(GetDataMessage(items))
        resumeWaiting()
        resetTimer()
    }

    override fun handleTimeout() {
        if (blockHashes.isEmpty()) {
            listener?.onTaskCompleted(this)
        } else {
            listener?.onTaskFailed(this, MerkleBlockNotReceived())
        }
    }

    override fun checkTimeout() {
        pauseWaiting()

        if (totalWaitingTime < 1000) {
            return resumeWaiting()
        }

        val awaitingSeconds = (totalWaitingTime / 1000.0)
        val minMerkleBlocks = (minMerkleBlocks * awaitingSeconds).roundToInt()
        val minTransactions = (minTransactions * awaitingSeconds).roundToInt()
        val minReceiveBytes = (minReceiveBytes * awaitingSeconds).roundToInt()

        if (minMerkleBlocks > receivedMerkleBlocks && minTransactions > receivedTransactions && minReceiveBytes > receivedBytes) {
            maxWarningCount -= 1
        }

        if (maxWarningCount < 1) {
            listener?.onTaskFailed(this, PeerTooSlow(
                    "Received ${receivedMerkleBlocks / totalWaitingTime} blocks, " +
                            "${receivedTransactions / totalWaitingTime} txs and $receivedBytes bytes per second; " +
                            "Required ${minMerkleBlocks / totalWaitingTime} blocks, " +
                            "${minTransactions / totalWaitingTime} txs or ${minReceiveBytes / totalWaitingTime} bytes per second"
            ))
        }

        resumeWaiting()
        totalWaitingTime = 0
        receivedMerkleBlocks = 0
        receivedTransactions = 0
        receivedBytes = 0
    }

    override fun handleMessage(message: IMessage): Boolean {
        pauseWaiting()

        val canHandleMessage = when (message) {
            is MerkleBlockMessage -> {
                receivedMerkleBlocks += 1
                receivedTransactions += message.txCount
                handleMerkleBlock(merkleBlockExtractor.extract(message))
            }
            is TransactionMessage -> {
                receivedBytes += message.size
                handleTransaction(message.transaction)
            }
            else -> false
        }

        resumeWaiting()

        return canHandleMessage
    }

    private fun handleMerkleBlock(merkleBlock: MerkleBlock): Boolean {
        val blockHash = blockHashes.find { merkleBlock.blockHash.contentEquals(it.headerHash) }
                ?: return false

        merkleBlock.height = if (blockHash.height > 0) blockHash.height else null

        if (merkleBlock.complete) {
            handleCompletedMerkleBlock(merkleBlock)
        } else {
            pendingMerkleBlocks.add(merkleBlock)
        }

        return true
    }

    private fun handleTransaction(transaction: FullTransaction): Boolean {
        val block = pendingMerkleBlocks.find { it.associatedTransactionHashes[HashBytes(transaction.header.hash)] == true }
                ?: return false

        block.associatedTransactions.add(transaction)

        if (block.complete) {
            pendingMerkleBlocks.remove(block)
            handleCompletedMerkleBlock(block)
        }

        return true
    }

    private fun handleCompletedMerkleBlock(merkleBlock: MerkleBlock) {
        blockHashes.find { it.headerHash.contentEquals(merkleBlock.blockHash) }?.let {
            blockHashes.remove(it)
        }

        try {
            merkleBlockHandler.handleMerkleBlock(merkleBlock)
        } catch (e: Exception) {
            listener?.onTaskFailed(this, e)
        }

        if (blockHashes.isEmpty()) {
            listener?.onTaskCompleted(this)
        }
    }

    private fun pauseWaiting() {
        totalWaitingTime += System.currentTimeMillis() - waitingStartTime

        if (pendingMerkleBlocks.size > 0 && !firstResponseReceived) {
            totalWaitingTime /= 2
            firstResponseReceived = true
        }
    }

    private fun resumeWaiting() {
        waitingStartTime = System.currentTimeMillis()
    }

    class MerkleBlockNotReceived : Exception("Merkle blocks are not received")
    class PeerTooSlow(e: String) : Exception(e)
}
