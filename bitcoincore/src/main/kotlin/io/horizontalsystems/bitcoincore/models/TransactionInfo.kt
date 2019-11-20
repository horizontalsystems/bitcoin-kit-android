package io.horizontalsystems.bitcoincore.models

import io.horizontalsystems.bitcoincore.core.IPluginOutputData

open class TransactionInfo(
        val transactionHash: String,
        val transactionIndex: Int,
        val from: List<TransactionAddress>,
        val to: List<TransactionAddress>,
        val amount: Long,
        val fee: Long?,
        val blockHeight: Int?,
        val timestamp: Long,
        val status: TransactionStatus
)

enum class TransactionStatus(val code: Int) {
    NEW(Transaction.Status.NEW),
    RELAYED(Transaction.Status.RELAYED),
    INVALID(Transaction.Status.INVALID);

    companion object {
        private val values = values()

        fun getByCode(code: Int): TransactionStatus? = values.firstOrNull { it.code == code }
    }
}

data class TransactionAddress(
        val address: String,
        val mine: Boolean,
        val pluginData: Map<Byte, IPluginOutputData>?
)

data class BlockInfo(
        val headerHash: String,
        val height: Int,
        val timestamp: Long
)

data class BalanceInfo(val spendable: Long, val unspendable: Long)
