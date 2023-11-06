package io.horizontalsystems.bitcoincore.apisync.model

data class TransactionItem(
    val blockHash: String,
    val blockHeight: Int,
    val addressItems: MutableList<AddressItem>
)
