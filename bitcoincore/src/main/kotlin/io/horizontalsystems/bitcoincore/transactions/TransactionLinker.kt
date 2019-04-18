package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.storage.FullTransaction

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
