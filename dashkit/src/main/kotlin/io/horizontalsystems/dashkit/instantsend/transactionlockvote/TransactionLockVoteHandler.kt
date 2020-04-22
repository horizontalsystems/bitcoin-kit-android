package io.horizontalsystems.dashkit.instantsend.transactionlockvote

import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.dashkit.IInstantTransactionDelegate
import io.horizontalsystems.dashkit.instantsend.InstantTransactionManager
import io.horizontalsystems.dashkit.messages.TransactionLockVoteMessage
import io.horizontalsystems.dashkit.models.InstantTransactionInput
import java.util.logging.Logger

class TransactionLockVoteHandler(
        private val instantTransactionManager: InstantTransactionManager,
        private val lockVoteManager: TransactionLockVoteManager,
        private val requiredVoteCount: Int = 6
) {
    var delegate: IInstantTransactionDelegate? = null

    private val logger = Logger.getLogger("TransactionLockVoteHandler")

    fun handle(transaction: FullTransaction) {
        // check transaction already not in instant
        if (instantTransactionManager.isTransactionInstant(transaction.header.hash)) {
            return
        }

        // prepare instant inputs for ix
        val inputs = instantTransactionManager.instantTransactionInputs(transaction.header.hash, transaction)

        // poll relayed lock votes to update inputs
        val relayedVotes = lockVoteManager.takeRelayedLockVotes(transaction.header.hash)
        relayedVotes.forEach { vote ->
            handle(vote, inputs)
        }
    }

    fun handle(lockVote: TransactionLockVoteMessage) {
        // check transaction already not in instant
        if (instantTransactionManager.isTransactionInstant(lockVote.txHash)) {
            return
        }
        if (lockVoteManager.processed(lockVote.hash)) {
            return
        }

        val inputs = instantTransactionManager.instantTransactionInputs(lockVote.txHash, null)
        if (inputs.isEmpty()) {
            lockVoteManager.addRelayed(lockVote)
            return
        }

        handle(lockVote, inputs)

    }

    private fun handle(lockVote: TransactionLockVoteMessage, instantInputs: List<InstantTransactionInput>) {
        lockVoteManager.addChecked(lockVote)
        // ignore votes for inputs which already has 6 votes
        val input = instantInputs.firstOrNull { it.inputTxHash.contentEquals(lockVote.outpoint.txHash) }

        if (input == null || input.voteCount >= requiredVoteCount) {
            return
        }

        try {
            lockVoteManager.validate(lockVote)
            instantTransactionManager.updateInput(lockVote.outpoint.txHash, instantInputs)

            if (instantTransactionManager.isTransactionInstant(lockVote.txHash)) {
                delegate?.onUpdateInstant(lockVote.txHash)
            }
        } catch(e: Exception) {
            logger.severe(e.message)
        }
    }
}
