package io.horizontalsystems.bitcoinkit.demo

sealed class FeePriority {
    object Low : FeePriority()
    object Medium : FeePriority()
    object High : FeePriority()
}
