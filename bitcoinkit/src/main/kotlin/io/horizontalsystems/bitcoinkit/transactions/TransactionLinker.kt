package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.storage.FullTransaction

class TransactionLinker(private val storage: IStorage) {

    fun handle(transaction: FullTransaction) {
        for (input in transaction.inputs) {
            val previousOutput = storage.getPreviousOutput(input = input) ?: continue
            if (previousOutput.publicKey(storage) == null) {
                continue
            }

            transaction.header.isMine = true
            transaction.header.isOutgoing = true
        }
    }
}
