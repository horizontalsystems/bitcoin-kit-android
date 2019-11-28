package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.bitcoincore.storage.FullTransactionInfo

class BaseTransactionInfoConverter(private val pluginManager: PluginManager) {

    fun transactionInfo(fullTransaction: FullTransactionInfo): TransactionInfo {
        val transaction = fullTransaction.header

        if (transaction.status == Transaction.Status.INVALID) {
            (transaction as? InvalidTransaction)?.let {
                return getInvalidTransactionInfo(it)
            }
        }

        var totalMineInput = 0L
        var totalMineOutput = 0L
        val fromAddresses = mutableListOf<TransactionAddress>()
        val toAddresses = mutableListOf<TransactionAddress>()

        var hasOnlyMyInputs = true

        fullTransaction.inputs.forEach { input ->
            var mine = false

            if (input.previousOutput?.publicKeyPath != null) {
                totalMineInput += input.previousOutput.value
                mine = true
            } else {
                hasOnlyMyInputs = false
            }

            input.input.address?.let { address ->
                fromAddresses.add(TransactionAddress(address, mine, null))
            }
        }

        fullTransaction.outputs.forEach { output ->
            var mine = false

            if (output.publicKeyPath != null) {
                totalMineOutput += output.value
                mine = true
            }

            output.address?.let { address ->
                toAddresses.add(TransactionAddress(address, mine, output.pluginId, pluginManager.parsePluginData(output, transaction.timestamp), output.pluginData))
            }
        }

        var fee: Long? = null
        var amount = totalMineOutput - totalMineInput

        if (hasOnlyMyInputs) {
            val outputsSum = fullTransaction.outputs.sumByDouble { it.value.toDouble() }.toLong()

            fee = totalMineInput - outputsSum
            amount += fee
        }

        return TransactionInfo(
                transactionHash = transaction.hash.toReversedHex(),
                transactionIndex = transaction.order,
                from = fromAddresses,
                to = toAddresses,
                amount = amount,
                fee = fee,
                blockHeight = fullTransaction.block?.height,
                timestamp = transaction.timestamp,
                status = TransactionStatus.getByCode(transaction.status) ?: TransactionStatus.NEW
        )
    }

    private fun getInvalidTransactionInfo(transaction: InvalidTransaction): TransactionInfo {
        return try {
            TransactionInfo(transaction.serializedTxInfo)
        } catch (ex: Exception) {
            TransactionInfo(
                    transactionHash = transaction.hash.toReversedHex(),
                    transactionIndex = transaction.order,
                    timestamp = transaction.timestamp,
                    status = TransactionStatus.INVALID,
                    from = listOf(),
                    to = listOf(),
                    amount = 0,
                    fee = 0,
                    blockHeight = null
            )
        }
    }

}