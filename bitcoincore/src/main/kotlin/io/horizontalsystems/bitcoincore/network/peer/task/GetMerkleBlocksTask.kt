package io.horizontalsystems.bitcoincore.network.peer.task

import io.horizontalsystems.bitcoincore.core.HashBytes
import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.models.InventoryItem
import io.horizontalsystems.bitcoincore.models.MerkleBlock
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import java.util.concurrent.TimeUnit

class GetMerkleBlocksTask(hashes: List<BlockHash>, private val merkleBlockHandler: MerkleBlockHandler) : PeerTask() {

    interface MerkleBlockHandler {
        fun handleMerkleBlock(merkleBlock: MerkleBlock)
    }

    private var blockHashes = hashes.toMutableList()
    private var pendingMerkleBlocks = mutableListOf<MerkleBlock>()

    init {
        allowedIdleTime = TimeUnit.SECONDS.toMillis(5)
    }

    override fun start() {
        val items = blockHashes.map { hash ->
            InventoryItem(InventoryItem.MSG_FILTERED_BLOCK, hash.headerHash)
        }

        requester?.getData(items)
        resetTimer()
    }

    override fun handleMerkleBlock(merkleBlock: MerkleBlock): Boolean {
        val blockHash = blockHashes.find { merkleBlock.blockHash.contentEquals(it.headerHash) }
                ?: return false

        resetTimer()

        merkleBlock.height = if (blockHash.height > 0) blockHash.height else null

        if (merkleBlock.complete) {
            handleCompletedMerkleBlock(merkleBlock)
        } else {
            pendingMerkleBlocks.add(merkleBlock)
        }

        return true
    }

    override fun handleTransaction(transaction: FullTransaction): Boolean {
        val block = pendingMerkleBlocks.find { it.associatedTransactionHashes[HashBytes(transaction.header.hash)] == true }
                ?: return false

        resetTimer()

        block.associatedTransactions.add(transaction)

        if (block.complete) {
            pendingMerkleBlocks.remove(block)
            handleCompletedMerkleBlock(block)
        }

        return true
    }

    override fun handleTimeout() {
        if (blockHashes.isEmpty()) {
            listener?.onTaskCompleted(this)
        } else {
            listener?.onTaskFailed(this, MerkleBlockNotReceived())
        }
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

    class MerkleBlockNotReceived : Exception("Merkle blocks are not received")

}
