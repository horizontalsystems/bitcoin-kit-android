package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.models.Transaction
import io.realm.Realm

class TransactionLinker {

    fun handle(transaction: Transaction, realm: Realm) {
        transaction.inputs.forEach { input ->
            val previousTransaction = realm.where(Transaction::class.java)
                    .equalTo("hashHexReversed", input.previousOutputHexReversed)
                    .findFirst()

            if (previousTransaction != null && previousTransaction.outputs.size > input.previousOutputIndex) {
                input.previousOutput = previousTransaction.outputs[input.previousOutputIndex.toInt()]
                transaction.isMine = true
            }
        }
    }

}
