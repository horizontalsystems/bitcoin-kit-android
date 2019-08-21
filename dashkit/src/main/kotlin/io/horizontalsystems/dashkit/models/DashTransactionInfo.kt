package io.horizontalsystems.dashkit.models

import io.horizontalsystems.bitcoincore.models.TransactionAddress
import io.horizontalsystems.bitcoincore.models.TransactionInfo

class DashTransactionInfo(
        transactionHash: String,
        transactionIndex: Int,
        from: List<TransactionAddress>,
        to: List<TransactionAddress>,
        amount: Long,
        fee: Long?,
        blockHeight: Int?,
        timestamp: Long,
        var instantTx: Boolean = false
) : TransactionInfo(transactionHash, transactionIndex, from, to, amount, fee, blockHeight, timestamp)
