package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.hdwallet.PublicKey
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.models.TransactionInput
import io.realm.Realm

class TransactionLinker {
    fun handle(transaction: Transaction, realm: Realm) {
        linkInputs(transaction, realm)
        linkOutputs(transaction, realm)
    }

    private fun linkInputs(transaction: Transaction, realm: Realm) {
        transaction.inputs.forEach { input ->
            val previousTransaction = realm.where(Transaction::class.java)
                    .equalTo("hashHexReversed", input.previousOutputHexReversed)
                    .findFirst()

            if (previousTransaction != null && previousTransaction.outputs.size > input.previousOutputIndex) {
                input.previousOutput = previousTransaction.outputs[input.previousOutputIndex.toInt()]

                if (input.previousOutput?.publicKey != null) {
                    transaction.isMine = true
                }
            }
        }
    }

    private fun linkOutputs(transaction: Transaction, realm: Realm) {
        transaction.outputs.forEach { output ->
            output.keyHash?.let {
                val pubKey = realm.where(PublicKey::class.java)
                        .equalTo("publicKeyHash", output.keyHash)
                        .findFirst()

                if (pubKey != null) {
                    transaction.isMine = true
                    output.publicKey = pubKey
                }
            }

            val input = realm.where(TransactionInput::class.java)
                    .equalTo("previousOutputHexReversed", transaction.hashHexReversed)
                    .equalTo("previousOutputIndex", output.index)
                    .findFirst()

            if (input != null) {
                input.previousOutput = output

                if (output.publicKey != null) {
                    input.transaction?.let { it.isMine = true }
                }
            }
        }
    }
}
