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

        newBlockHashes.forEach { newBlockHash ->
            blockHashes.forEach { blockHash ->
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
