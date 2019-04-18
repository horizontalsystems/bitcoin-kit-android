package io.horizontalsystems.bitcoincore.models

data class TransactionInfo(
        val transactionHash: String,
        val from: List<TransactionAddress>,
        val to: List<TransactionAddress>,
        val amount: Long,
        val blockHeight: Int?,
        val timestamp: Long
)

data class TransactionAddress(
        val address: String,
        val mine: Boolean
)

data class BlockInfo(
        val headerHash: String,
        val height: Int,
        val timestamp: Long?
)
