package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.bitcoincore.storage.FullTransactionInfo

class BaseTransactionInfoConverter(private val pluginManager: PluginManager) {

    fun transactionInfo(fullTransaction: FullTransactionInfo): TransactionInfo {
        val transaction = fullTransaction.header

        if (transaction.status == Transaction.Status.INVALID) {
            (transaction as? InvalidTransaction)?.let {
                return getInvalidTransactionInfo(it, fullTransaction.metadata)
            }
        }

        val inputsInfo = mutableListOf<TransactionInputInfo>()
        val outputsInfo = mutableListOf<TransactionOutputInfo>()

        fullTransaction.inputs.forEach { input ->
            var mine = false
            var value: Long? = null

            if (input.previousOutput != null) {
                value = input.previousOutput.value
                if (input.previousOutput.publicKeyPath != null) {
                    mine = true
                }
            }

            inputsInfo.add(TransactionInputInfo(mine, value, input.input.address))
        }

        fullTransaction.outputs.forEach { output ->
            val outputInfo = TransactionOutputInfo(mine = output.publicKeyPath != null,
                    changeOutput = output.changeOutput,
                    value = output.value,
                    address = output.address,
                    pluginId = output.pluginId,
                    pluginDataString = output.pluginData,
                    pluginData = pluginManager.parsePluginData(output, transaction.timestamp))

            outputsInfo.add(outputInfo)
        }

        return TransactionInfo(
            uid = transaction.uid,
            transactionHash = transaction.hash.toReversedHex(),
            transactionIndex = transaction.order,
            inputs = inputsInfo,
            outputs = outputsInfo,
            amount = fullTransaction.metadata.amount,
            type = fullTransaction.metadata.type,
            fee = fullTransaction.metadata.fee,
            blockHeight = fullTransaction.block?.height,
            timestamp = transaction.timestamp,
            status = TransactionStatus.getByCode(transaction.status) ?: TransactionStatus.NEW,
            conflictingTxHash = transaction.conflictingTxHash?.toReversedHex()
        )
    }

    private fun getInvalidTransactionInfo(
        transaction: InvalidTransaction,
        metadata: TransactionMetadata
    ): TransactionInfo {
        return try {
            TransactionInfo(transaction.serializedTxInfo)
        } catch (ex: Exception) {
            TransactionInfo(
                uid = transaction.uid,
                transactionHash = transaction.hash.toReversedHex(),
                transactionIndex = transaction.order,
                inputs = listOf(),
                outputs = listOf(),
                amount = metadata.amount,
                type = metadata.type,
                fee = metadata.fee,
                blockHeight = null,
                timestamp = transaction.timestamp,
                status = TransactionStatus.INVALID
            )
        }
    }

}