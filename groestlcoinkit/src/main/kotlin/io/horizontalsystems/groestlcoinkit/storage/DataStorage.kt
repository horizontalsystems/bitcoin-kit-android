package io.horizontalsystems.groestlcoinkit.storage

import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.utils.HashUtils
import io.horizontalsystems.groestlcoinkit.serializers.GroestlcoinTransactionSerializer

class GroestlcoinFullTransaction(header: Transaction, inputs: List<TransactionInput>, outputs: List<TransactionOutput>) :
    FullTransaction(header, inputs, outputs) {

    init {
        header.hash = HashUtils.sha256(GroestlcoinTransactionSerializer.serialize(this, withWitness = false))

        inputs.forEach {
            it.transactionHash = header.hash
        }
        outputs.forEach {
            it.transactionHash = header.hash
        }
    }

}