package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.storage.FullTransactionInfo

class TransactionInfoConverter : ITransactionInfoConverter {
    override lateinit var baseConverter: BaseTransactionInfoConverter

    override fun transactionInfo(transactionForInfo: FullTransactionInfo): TransactionInfo {
        return baseConverter.transactionInfo(transactionForInfo)
    }
}
