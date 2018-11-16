package io.horizontalsystems.bitcoinkit.network.PeerTask

import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.InventoryItem
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.models.Transaction

class GetMerkleBlocksTask(hashes: List<BlockHash>) : PeerTask() {

    private var blockHashes = hashes.toMutableList()
    private var pendingMerkleBlocks = mutableListOf<MerkleBlock>()
    private val pingNonce = (Math.random() * Long.MAX_VALUE).toLong()

    override fun start() {
        val items = blockHashes.map { hash ->
            InventoryItem(InventoryItem.MSG_FILTERED_BLOCK, hash.headerHash)
        }

        requester?.getData(items)
        requester?.ping(pingNonce)
    }

    override fun handleMerkleBlock(merkleBlock: MerkleBlock): Boolean {
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

    override fun handleTransaction(transaction: Transaction): Boolean {
        val block = pendingMerkleBlocks.find { it.associatedTransactionHexes.contains(transaction.hash.toHexString()) }
                ?: return false

        block.associatedTransactions.add(transaction)

        if (block.complete) {
            pendingMerkleBlocks.remove(block)
            handleCompletedMerkleBlock(block)
        }

        return true
    }

    override fun handlePong(nonce: Long): Boolean {
        if (nonce == pingNonce) {
            listener?.onTaskFailed(this, MerkleBlockNotReceived())
            return true
        }

        return false
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
