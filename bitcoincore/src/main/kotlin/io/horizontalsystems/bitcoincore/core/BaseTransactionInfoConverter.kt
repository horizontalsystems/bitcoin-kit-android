package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.models.TransactionAddress
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.storage.FullTransactionInfo

class BaseTransactionInfoConverter {

    fun transactionInfo(fullTransaction: FullTransactionInfo): TransactionInfo {
        val transaction = fullTransaction.header

        var totalMineInput = 0L
        var totalMineOutput = 0L
        val fromAddresses = mutableListOf<TransactionAddress>()
        val toAddresses = mutableListOf<TransactionAddress>()

        fullTransaction.inputs.forEach { input ->
            var mine = false

            if (input.previousOutput?.publicKeyPath != null) {
                totalMineInput += input.previousOutput.value
                mine = true

            }

            input.input.address?.let { address ->
                fromAddresses.add(TransactionAddress(address, mine = mine))
            }
        }

        fullTransaction.outputs.forEach { output ->
            var mine = false

            if (output.publicKeyPath != null) {
                totalMineOutput += output.value
                mine = true
            }

            output.address?.let { address ->
                toAddresses.add(TransactionAddress(address, mine))
            }
        }

        return TransactionInfo(
                transactionHash = transaction.hash.toReversedHex(),
                transactionIndex = transaction.order,
                from = fromAddresses,
                to = toAddresses,
                amount = totalMineOutput - totalMineInput,
                blockHeight = fullTransaction.block?.height,
                timestamp = transaction.timestamp
        )
    }

}