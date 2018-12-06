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
                val previousOutput = previousTransaction.outputs[input.previousOutputIndex.toInt()]
                if (previousOutput?.publicKey != null) {
                    input.previousOutput = previousOutput
                    input.address = previousOutput.address
                    input.keyHash = previousOutput.keyHash
                    transaction.isMine = true
                    transaction.isOutgoing = true
                }
            }
        }
    }

}
