package bitcoin.wallet.kit.network.PeerTask

import bitcoin.wallet.kit.models.InventoryItem

class GetBlockHashesTask(private val blockLocatorHashes: List<ByteArray>) : PeerTask() {

    var blockHashes = listOf<ByteArray>()
    private val pingNonce = (Math.random() * Long.MAX_VALUE).toLong()

    override fun start() {
        requester?.getBlocks(blockLocatorHashes)
        requester?.ping(pingNonce)
    }

    override fun handleInventoryItems(items: List<InventoryItem>): Boolean {
        val newBlockHashes = items.filter { it.type == InventoryItem.MSG_BLOCK }.map { it.hash }

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

        return newBlockHashes.isNotEmpty()
    }

    override fun handlePong(nonce: Long): Boolean {
        if (nonce == pingNonce) {
            delegate?.onTaskCompleted(this)
            return true
        }

        return false
    }
}
