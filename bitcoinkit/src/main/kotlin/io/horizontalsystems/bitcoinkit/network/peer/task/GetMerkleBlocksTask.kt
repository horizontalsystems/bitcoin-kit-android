package io.horizontalsystems.bitcoinkit.network.peer.task

import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.InventoryItem
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.models.Transaction
import java.util.concurrent.TimeUnit

class GetMerkleBlocksTask(hashes: List<BlockHash>) : PeerTask() {
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

    override fun handleTransaction(transaction: Transaction): Boolean {
        val block = pendingMerkleBlocks.find { it.associatedTransactionHexes.contains(transaction.hash.toHexString()) }
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

        listener?.handleMerkleBlock(merkleBlock)

        if (blockHashes.isEmpty()) {
            listener?.onTaskCompleted(this)
        }
    }

    class MerkleBlockNotReceived : Exception("Merkle blocks are not received")

}
