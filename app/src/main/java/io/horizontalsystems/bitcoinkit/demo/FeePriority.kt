package io.horizontalsystems.bitcoinkit.demo

sealed class FeePriority(val feeRate: Int) {
    object Low : FeePriority(5)
    object Medium : FeePriority(10)
    object High : FeePriority(15)
}
