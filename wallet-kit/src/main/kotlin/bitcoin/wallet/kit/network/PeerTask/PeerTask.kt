package bitcoin.wallet.kit.network.PeerTask

import bitcoin.wallet.kit.models.Header
import bitcoin.wallet.kit.models.InventoryItem
import bitcoin.wallet.kit.models.MerkleBlock
import bitcoin.wallet.kit.models.Transaction

open class PeerTask {
    var requester: IPeerTaskRequester? = null
    var delegate: IPeerTaskDelegate? = null

    open fun start() = Unit

    open fun handleBlockHeaders(blockHeaders: List<Header>): Boolean {
        return false
    }

    open fun handleMerkleBlock(merkleBlock: MerkleBlock): Boolean {
        return false
    }

    open fun handleTransaction(transaction: Transaction): Boolean {
        return false
    }

    open fun handlePong(nonce: Long): Boolean {
        return false
    }

    open fun handleGetDataInventoryItem(item: InventoryItem): Boolean {
        return false
    }

    open fun handleInventoryItems(items: List<InventoryItem>): Boolean {
        return false
    }

    open fun handleRelayedTransaction(hash: ByteArray): Boolean {
        return false
    }

    open fun isRequestingInventory(hash: ByteArray): Boolean {
        return false
    }
}

interface IPeerTaskDelegate {
    fun onTaskCompleted(task: PeerTask)
    fun handleMerkleBlock(merkleBlock: MerkleBlock, fullBlock: Boolean)
}

interface IPeerTaskRequester {
    fun getBlocks(hashes: List<ByteArray>)
    fun ping(nonce: Long)
    fun getData(items: List<InventoryItem>)
    fun sendTransactionInventory(hash: ByteArray)
    fun send(transaction: Transaction)
}
