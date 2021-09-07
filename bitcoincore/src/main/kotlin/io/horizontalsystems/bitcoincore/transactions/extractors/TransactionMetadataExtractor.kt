package io.horizontalsystems.bitcoincore.transactions.extractors

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import kotlin.math.absoluteValue

class TransactionMetadataExtractor(
    private val myOutputsCache: MyOutputsCache,
    private val outputProvider: ITransactionOutputProvider
) {

    fun extract(transaction: FullTransaction) {

        var myInputsTotalValue = 0L
        var myOutputsTotalValue = 0L
        var myChangeOutputsTotalValue = 0L
        var outputsTotalValue = 0L
        var allInputsMine = true

        for (input in transaction.inputs) {
            val value = myOutputsCache.valueSpentBy(input)
            if (value != null) {
                myInputsTotalValue += value
            } else {
                allInputsMine = false
            }
        }

        for (output in transaction.outputs) {
            if (output.value <= 0) continue

            outputsTotalValue += output.value

            if (output.publicKeyPath != null) {
                myOutputsTotalValue += output.value
                if (output.changeOutput) {
                    myChangeOutputsTotalValue += output.value
                }
            }
        }

        if (myInputsTotalValue == 0L && myOutputsTotalValue == 0L) {
            return
        }

        transaction.header.isMine = true
        if (myInputsTotalValue > 0) {
            transaction.header.isOutgoing = true
        }

        var amount = myOutputsTotalValue - myInputsTotalValue
        var fee: Long? = null

        if (allInputsMine) {
            fee = myInputsTotalValue - outputsTotalValue
            amount += fee
        } else {
            var inputsTotalValue = 0L
            var allInputsHaveValue = true
            for (input in transaction.inputs) {
                val previousOutput = outputProvider.get(input.previousOutputTxHash, input.previousOutputIndex.toInt())
                if (previousOutput != null) {
                    inputsTotalValue += previousOutput.value
                } else {
                    allInputsHaveValue = false
                    break
                }
            }

            fee = if (allInputsHaveValue) inputsTotalValue - outputsTotalValue else null
        }

        if (amount > 0) {
            transaction.metadata.amount = amount
            transaction.metadata.type = TransactionType.Incoming
        } else if (amount < 0) {
            transaction.metadata.amount = amount.absoluteValue
            transaction.metadata.type = TransactionType.Outgoing
        } else {
            transaction.metadata.amount = (myOutputsTotalValue - myChangeOutputsTotalValue).absoluteValue
            transaction.metadata.type = TransactionType.SentToSelf
        }
        transaction.metadata.fee = fee

        if (myOutputsTotalValue > 0) {
            myOutputsCache.add(transaction.outputs)
        }
    }
}

interface ITransactionOutputProvider {
    fun get(transactionHash: ByteArray, index: Int): TransactionOutput?
}

class TransactionOutputProvider(private val storage: IStorage): ITransactionOutputProvider {
    override fun get(transactionHash: ByteArray, index: Int): TransactionOutput? {
        return storage.getOutput(transactionHash, index)
    }
}
