package com.anwang.safewallet.safekit.model

data class SafeInfo(
    val safe_usdt: String,
    val minamount: String,
    val eth: ChainInfo,
    val bsc: ChainInfo
)

data class ChainInfo(
    val price: String,
    val gas_price_gwei: String,
    val safe_fee: String,
    val safe2eth: Boolean,
    val eth2safe: Boolean
)


