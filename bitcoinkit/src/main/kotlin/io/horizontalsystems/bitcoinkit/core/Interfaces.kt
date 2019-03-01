package io.horizontalsystems.bitcoinkit.core

import io.horizontalsystems.bitcoinkit.models.FeeRate

interface IStorage {
    val feeRate: FeeRate?
    fun setFeeRate(feeRate: FeeRate)

    val initialRestored: Boolean?
    fun setInitialRestored(isRestored: Boolean)

    fun clear()
}
