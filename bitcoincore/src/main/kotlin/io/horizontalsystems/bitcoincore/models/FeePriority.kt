package io.horizontalsystems.bitcoincore.models

sealed class FeePriority {
    object Lowest : FeePriority()
    object Low : FeePriority()
    object Medium : FeePriority()
    object High : FeePriority()
    object Highest : FeePriority()
    class Custom(val feeRate: Int) : FeePriority()
}
