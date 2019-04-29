package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.storage.FullTransaction

class TransactionLinker(private val storage: IStorage) {

    fun handle(transaction: FullTransaction) {
        for (input in transaction.inputs) {
            if (storage.previousOutputWithPubKeyExists(input)) {
                transaction.header.isMine = true
                transaction.header.isOutgoing = true
            }
        }
    }
}
