package com.anwang.safewallet.safekit.model

data class SafeNet(
    val safe_usdt: String,
    val minamount: String,
    val eth: SafeChain,
    val bsc: SafeChain
)

data class SafeChain(
    val price: String,
    val gas_price_gwei: String,
    val safe_fee: String,
    val safe2eth: Boolean,
    val eth2safe: Boolean
)


