package io.horizontalsystems.dashkit.core

import io.horizontalsystems.bitcoincore.core.BaseTransactionInfoConverter
import io.horizontalsystems.bitcoincore.core.ITransactionInfoConverter
import io.horizontalsystems.bitcoincore.storage.FullTransactionInfo
import io.horizontalsystems.dashkit.instantsend.InstantTransactionManager
import io.horizontalsystems.dashkit.models.DashTransactionInfo

class DashTransactionInfoConverter(private val instantTransactionManager: InstantTransactionManager) : ITransactionInfoConverter {
    override lateinit var baseConverter: BaseTransactionInfoConverter

    override fun transactionInfo(transactionForInfo: FullTransactionInfo): DashTransactionInfo {
        val txInfo = baseConverter.transactionInfo(transactionForInfo)
        return DashTransactionInfo(
                txInfo.transactionHash,
                txInfo.transactionIndex,
                txInfo.from,
                txInfo.to,
                txInfo.amount,
                txInfo.fee,
                txInfo.blockHeight,
                txInfo.timestamp,
                instantTransactionManager.isTransactionInstant(transactionForInfo.header.hash),
                txInfo.status
        )
    }
}