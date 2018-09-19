package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.scripts.Script

class TransactionExtractor {

    fun extract(transaction: Transaction) {
        transaction.outputs.forEach { output ->
            val script = Script(output.lockingScript)
            val pkHash = script.getPubKeyHash()
            if (pkHash != null) {
                output.scriptType = script.getScriptType()
                output.keyHash = pkHash
            }
        }

        transaction.inputs.forEach { input ->
        }
    }
}
