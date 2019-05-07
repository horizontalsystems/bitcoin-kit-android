package io.horizontalsystems.dashkit

import io.horizontalsystems.bitcoincore.models.InventoryItem
import io.horizontalsystems.bitcoincore.network.peer.IInventoryItemsHandler
import io.horizontalsystems.bitcoincore.network.peer.IPeerTaskHandler
import io.horizontalsystems.bitcoincore.network.peer.Peer
import io.horizontalsystems.bitcoincore.network.peer.task.PeerTask
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.transactions.TransactionSyncer
import io.horizontalsystems.dashkit.instantsend.InstantTransactionManager
import io.horizontalsystems.dashkit.instantsend.TransactionLockVoteManager
import io.horizontalsystems.dashkit.messages.TransactionLockVoteMessage
import io.horizontalsystems.dashkit.models.InstantTransactionInput
import io.horizontalsystems.dashkit.tasks.RequestTransactionLockRequestsTask
import io.horizontalsystems.dashkit.tasks.RequestTransactionLockVotesTask
import java.util.concurrent.Executors
import java.util.logging.Logger

class InstantSend(
        private val transactionSyncer: TransactionSyncer,
        private val lockVoteManager: TransactionLockVoteManager,
        private val instantTransactionManager: InstantTransactionManager
) : IInventoryItemsHandler, IPeerTaskHandler {

    var delegate: IInstantTransactionDelegate? = null

    private val logger = Logger.getLogger("InstantSend")
    private val dispatchQueue = Executors.newSingleThreadExecutor()

    override fun handleInventoryItems(peer: Peer, inventoryItems: List<InventoryItem>) {
        val transactionLockRequests = mutableListOf<ByteArray>()
        val transactionLockVotes = mutableListOf<ByteArray>()

        inventoryItems.forEach { item ->
            when (item.type) {
                InventoryType.MSG_TXLOCK_REQUEST -> {
                    transactionLockRequests.add(item.hash)
                }
                InventoryType.MSG_TXLOCK_VOTE -> {
                    transactionLockVotes.add(item.hash)
                }
            }
        }

        if (transactionLockRequests.isNotEmpty()) {
            peer.addTask(RequestTransactionLockRequestsTask(transactionLockRequests))
        }

        if (transactionLockVotes.isNotEmpty()) {
            peer.addTask(RequestTransactionLockVotesTask(transactionLockVotes))
        }

    }

    override fun handleCompletedTask(peer: Peer, task: PeerTask): Boolean {
        return when (task) {
            is RequestTransactionLockRequestsTask -> {
                dispatchQueue.execute {
                    handleTransactions(task.transactions)
                }
                true
            }
            is RequestTransactionLockVotesTask -> {
                dispatchQueue.execute {
                    handleTransactionLockVotes(task.transactionLockVotes)
                }
                true
            }
            else -> false
        }
    }

    private fun handleTransactions(transactions: List<FullTransaction>) {
        transactionSyncer.handleTransactions(transactions)

        for (transaction in transactions) {
            // check transaction already not in instant
            if (instantTransactionManager.isTransactionInstant(transaction.header.hash)) {
                continue
            }

            // prepare instant inputs for ix
            val inputs = instantTransactionManager.instantTransactionInputs(transaction.header.hash, transaction)

            // poll relayed lock votes to update inputs
            val relayedVotes = lockVoteManager.takeRelayedLockVotes(transaction.header.hash)
            relayedVotes.forEach { vote ->
                handleLockVote(vote, inputs)
            }
        }
    }

    private fun handleTransactionLockVotes(transactionLockVotes: List<TransactionLockVoteMessage>) {
        for (vote in transactionLockVotes) {
            // check transaction already not in instant
            if (instantTransactionManager.isTransactionInstant(vote.txHash)) {
                continue
            }
            if (lockVoteManager.processed(vote.hash)) {
                continue
            }

            val inputs = instantTransactionManager.instantTransactionInputs(vote.txHash, null)
            if (inputs.isEmpty()) {
                lockVoteManager.addRelayed(vote)
                continue
            }

            handleLockVote(vote, inputs)
        }
    }

    private fun handleLockVote(lockVote: TransactionLockVoteMessage, instantInputs: List<InstantTransactionInput>) {
        lockVoteManager.addChecked(lockVote)

        // ignore votes for inputs which already has 6 votes
        instantInputs.firstOrNull { it.inputTxHash.contentEquals(lockVote.outpoint.txHash) }?.let { input ->
            if (input.voteCount >= requiredVoteCount) {
                return
            }
        }

        try {
            lockVoteManager.validate(lockVote)
            instantTransactionManager.updateInput(lockVote.outpoint.txHash, instantInputs)

            val instant = instantTransactionManager.isTransactionInstant(lockVote.txHash)
            if (instant) {
                delegate?.onUpdateInstant(lockVote.txHash)
            }
        } catch (e: Exception) {
            logger?.severe("${e.message}")
        }
    }

    companion object {
        const val requiredVoteCount = 6
    }

}
