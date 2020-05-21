package io.horizontalsystems.bitcoincore.models

import androidx.room.Entity

@Entity
class InvalidTransaction() : Transaction() {

    constructor(transaction: Transaction, serializedTxInfo: String, rawTransaction: String?) : this() {
        uid = transaction.uid
        hash = transaction.hash
        blockHash = transaction.blockHash
        version = transaction.version
        lockTime = transaction.lockTime
        timestamp = transaction.timestamp
        order = transaction.order
        isMine = transaction.isMine
        isOutgoing = transaction.isOutgoing
        segwit = transaction.segwit
        status = Status.INVALID
        conflictingTxHash = transaction.conflictingTxHash

        this.serializedTxInfo = serializedTxInfo
        this.rawTransaction = rawTransaction
    }

}
