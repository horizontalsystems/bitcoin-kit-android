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

        val inputsInfo = mutableListOf<TransactionInputInfo>()
        val outputsInfo = mutableListOf<TransactionOutputInfo>()
        var inputsTotalValue = 0L
        var outputsTotalValue = 0L
        var allInputsHaveValue = true

        fullTransaction.inputs.forEach { input ->
            var mine = false
            var value: Long? = null

            if (input.previousOutput != null) {
                value = input.previousOutput.value
                if (input.previousOutput.publicKeyPath != null) {
                    mine = true
                }
            } else {
                allInputsHaveValue = false
            }

            inputsTotalValue += value ?: 0

            inputsInfo.add(TransactionInputInfo(mine, value, input.input.address))
        }

        fullTransaction.outputs.forEach { output ->
            outputsTotalValue += output.value

            val outputInfo = TransactionOutputInfo(mine = output.publicKeyPath != null,
                    changeOutput = output.changeOutput,
                    value = output.value,
                    address = output.address,
                    pluginId = output.pluginId,
                    pluginDataString = output.pluginData,
                    pluginData = pluginManager.parsePluginData(output, transaction.timestamp))

            outputsInfo.add(outputInfo)
        }

        val fee = if (allInputsHaveValue) inputsTotalValue - outputsTotalValue else null

        return TransactionInfo(
                uid = transaction.uid,
                transactionHash = transaction.hash.toReversedHex(),
                transactionIndex = transaction.order,
                inputs = inputsInfo,
                outputs = outputsInfo,
                fee = fee,
                blockHeight = fullTransaction.block?.height,
                timestamp = transaction.timestamp,
                status = TransactionStatus.getByCode(transaction.status) ?: TransactionStatus.NEW,
                conflictingTxHash = transaction.conflictingTxHash?.toReversedHex())
    }

    private fun getInvalidTransactionInfo(transaction: InvalidTransaction): TransactionInfo {
        return try {
            TransactionInfo(transaction.serializedTxInfo)
        } catch (ex: Exception) {
            TransactionInfo(
                    uid = transaction.uid,
                    transactionHash = transaction.hash.toReversedHex(),
                    transactionIndex = transaction.order,
                    timestamp = transaction.timestamp,
                    status = TransactionStatus.INVALID,
                    inputs = listOf(),
                    outputs = listOf(),
                    fee = null,
                    blockHeight = null)
        }
    }

}