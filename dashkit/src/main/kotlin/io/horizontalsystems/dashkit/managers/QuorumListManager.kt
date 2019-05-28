package io.horizontalsystems.dashkit.managers

import android.util.Log
import io.horizontalsystems.dashkit.IDashStorage
import io.horizontalsystems.dashkit.masternodelist.QuorumListMerkleRootCalculator
import io.horizontalsystems.dashkit.messages.MasternodeListDiffMessage

class QuorumListManager(
        private val storage: IDashStorage,
        private val quorumListMerkleRootCalculator: QuorumListMerkleRootCalculator,
        private val quorumSortedList: QuorumSortedList
) {
    open class ValidationError : Exception() {
        object WrongMerkleRootList : ValidationError()
    }

    @Throws(ValidationError::class)
    fun updateList(masternodeListDiffMessage: MasternodeListDiffMessage) {
        quorumSortedList.removeAll()

        //01. Create a copy of the active LLMQ sets which were given at "baseBlockHash". If “baseBlockHash” is all-zero, empty sets must be used.
        quorumSortedList.add(storage.quorums)
        //02. Delete all entries found in "deletedQuorums" from the corresponding active LLMQ sets.
        quorumSortedList.remove(masternodeListDiffMessage.deletedQuorums)
        //03. Verify each final commitment found in "newQuorums", by the same rules found in DIP6 - Long-Living Masternode Quorums. If any final commitment is invalid, abort the process and ask for diffs from another node.
        masternodeListDiffMessage.quorumList.forEach {
            //            TODO()
            Log.e("AAA", "Ask Toha to implement it")
        }
        //04. Add the LLMQ defined by the final commitments found in "newQuorums" to the corresponding active LLMQ sets.
        quorumSortedList.add(masternodeListDiffMessage.quorumList)

        masternodeListDiffMessage.cbTx.merkleRootQuorums?.let { merkleRootQuorums ->
            //05. Calculate the merkle root of the active LLMQ sets by following the “Calculating the merkle root of the active LLMQs” section
            val hash = quorumListMerkleRootCalculator.calculateMerkleRoot(quorumSortedList.quorums)

            //06. Compare the calculated merkle root with what is found in “cbTx”. If it does not match, abort the process and ask for diffs from another node.
            if (hash != null && !merkleRootQuorums.contentEquals(hash)) {
                throw ValidationError.WrongMerkleRootList
            }
        }

        //07. Store the new active LLMQ sets the same way the masternode list is stored.
        storage.quorums = quorumSortedList.quorums
    }

}
