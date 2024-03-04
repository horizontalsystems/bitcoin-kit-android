package io.horizontalsystems.bitcoincore.rbf

import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction

data class ReplacementTransaction(
    internal val mutableTransaction: MutableTransaction,
    val info: TransactionInfo,
    val replacedTransactionHashes: List<String>
)

data class ReplacementTransactionInfo(
    val originalTransactionSize: Long,
    val feeRange: LongRange
)
