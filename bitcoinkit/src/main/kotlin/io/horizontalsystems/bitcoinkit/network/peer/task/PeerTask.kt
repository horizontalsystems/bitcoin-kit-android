package io.horizontalsystems.bitcoinkit.network.peer.task

import io.horizontalsystems.bitcoinkit.models.Header
import io.horizontalsystems.bitcoinkit.models.InventoryItem
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.storage.FullTransaction
import java.util.*

open class PeerTask {

    interface Listener {
        fun onTaskCompleted(task: PeerTask)
        fun onTaskFailed(task: PeerTask, e: Exception)
        fun handleMerkleBlock(merkleBlock: MerkleBlock)
    }

    interface Requester {
        fun getBlocks(hashes: List<ByteArray>)
        fun ping(nonce: Long)
        fun getData(items: List<InventoryItem>)
        fun sendTransactionInventory(hash: ByteArray)
        fun send(transaction: FullTransaction)
    }

    var requester: Requester? = null
    var listener: Listener? = null
    protected var lastActiveTime: Long? = null
    protected var allowedIdleTime: Long? = null

    open fun start() = Unit

    open fun handleBlockHeaders(blockHeaders: List<Header>): Boolean {
        return false
    }

    open fun handleMerkleBlock(merkleBlock: MerkleBlock): Boolean {
        return false
    }

    open fun handleTransaction(transaction: FullTransaction): Boolean {
        return false
    }

    open fun handleGetDataInventoryItem(item: InventoryItem): Boolean {
        return false
    }

    open fun handleInventoryItems(items: List<InventoryItem>): Boolean {
        return false
    }

    open fun isRequestingInventory(hash: ByteArray): Boolean {
        return false
    }

    open fun handleTimeout() = Unit

    fun checkTimeout() {
        allowedIdleTime?.let { allowedIdleTime ->
            lastActiveTime?.let { lastActiveTime ->
                if (Date().time - lastActiveTime > allowedIdleTime) {
                    handleTimeout()
                }
            }
        }
    }

    fun resetTimer() {
        lastActiveTime = Date().time
    }

}
