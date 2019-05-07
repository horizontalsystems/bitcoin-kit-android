package io.horizontalsystems.dashkit.instantsend

import io.horizontalsystems.dashkit.messages.TransactionLockVoteMessage

class TransactionLockVoteManager(private val transactionLockVoteValidator: TransactionLockVoteValidator) {
    val relayedLockVotes = mutableListOf<TransactionLockVoteMessage>()
    val checkedLockVotes = mutableListOf<TransactionLockVoteMessage>()

    fun takeRelayedLockVotes(txHash: ByteArray): List<TransactionLockVoteMessage> {
        val votes = relayedLockVotes.filter {
            it.txHash.contentEquals(txHash)
        }
        relayedLockVotes.removeAll(votes)
        return votes
    }

    fun addRelayed(vote: TransactionLockVoteMessage) {
        relayedLockVotes.add(vote)
    }

    fun addChecked(vote: TransactionLockVoteMessage) {
        checkedLockVotes.add(vote)
    }

    fun removeCheckedLockVotes(txHash: ByteArray) {
        val index = checkedLockVotes.indexOfFirst { it.txHash.contentEquals(txHash) }
        if (index != -1) {
            checkedLockVotes.removeAt(index)
        }
    }

    fun processed(lvHash: ByteArray): Boolean {
        return relayedLockVotes.any { it.hash.contentEquals(lvHash) } || checkedLockVotes.any { it.hash.contentEquals(lvHash) }
    }

    @Throws
    fun validate(lockVote: TransactionLockVoteMessage) {
        // validate masternode in top 10 masternodes for quorumModifier
        transactionLockVoteValidator.validate(lockVote.quorumModifierHash, lockVote.masternodeProTxHash)
    }

}
