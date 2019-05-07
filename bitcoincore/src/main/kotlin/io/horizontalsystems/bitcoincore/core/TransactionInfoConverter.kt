package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.storage.FullTransactionInfo

class TransactionInfoConverter(private val baseTransactionInfoConverter: BaseTransactionInfoConverter) : ITransactionInfoConverter {

    override fun transactionInfo(transactionForInfo: FullTransactionInfo): TransactionInfo {
        return baseTransactionInfoConverter.transactionInfo(transactionForInfo)
    }
}
