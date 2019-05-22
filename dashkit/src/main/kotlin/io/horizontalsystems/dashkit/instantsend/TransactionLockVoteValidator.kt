package io.horizontalsystems.dashkit.instantsend

import io.horizontalsystems.bitcoincore.core.IHasher
import io.horizontalsystems.dashkit.DashKitErrors
import io.horizontalsystems.dashkit.IDashStorage

class TransactionLockVoteValidator(private val storage: IDashStorage, private val hasher: IHasher, private val bls: BLS) {

    companion object {
        private const val totalSignatures = 10
    }

    @Throws
    fun validate(quorumModifierHash: ByteArray, masternodeProTxHash: ByteArray, vchMasternodeSignature: ByteArray, hash: ByteArray) {
        val masternodes = storage.masternodes.filter { it.isValid }

        val quorumMasternodes = mutableListOf<QuorumMasternode>()

        // 1. Make list of masternodes with quorumHashes
        masternodes.forEach { masternode ->
            val quorumHash = hasher.hash(hasher.hash(masternode.proRegTxHash + masternode.confirmedHash) + quorumModifierHash).reversedArray() //Score calculated for littleEndiad (check last bytes, then previous and ...)
            quorumMasternodes.add(QuorumMasternode(quorumHash, masternode))
        }

        // 2. Sort descending
        quorumMasternodes.sortDescending()

        // 3. Find index for masternode
        val index = quorumMasternodes.indexOfFirst { it.masternode.proRegTxHash.contentEquals(masternodeProTxHash) }
        if (index == -1) {
            throw DashKitErrors.LockVoteValidation.MasternodeNotFound()
        }

        // 4. Check masternode in first 10 scores
        if (index > totalSignatures) {
            throw DashKitErrors.LockVoteValidation.MasternodeNotInTop()
        }

        // 5. Check signature
        val masternode = quorumMasternodes[index].masternode
        if (!bls.verifySignature(masternode.pubKeyOperator, vchMasternodeSignature, hash)) {
            throw DashKitErrors.LockVoteValidation.SignatureNotValid()
        }
    }
}
