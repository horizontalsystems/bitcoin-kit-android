package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.models.Transaction
import io.realm.Realm

class TransactionLinker {

    fun handle(transaction: Transaction, realm: Realm) {
        for (input in transaction.inputs) {
            val previousTransaction = realm.where(Transaction::class.java)
                    .equalTo("hashHexReversed", input.previousOutputHexReversed)
                    .findFirst()

            if (previousTransaction == null || previousTransaction.outputs.size <= input.previousOutputIndex) {
                continue
            }

            val previousOutput = previousTransaction.outputs[input.previousOutputIndex.toInt()]
            if (previousOutput?.publicKey == null) {
                continue
            }

            transaction.isMine = true
            transaction.isOutgoing = true

            // Link previousOutput to this input
            input.previousOutput = previousOutput
        }
    }

}
