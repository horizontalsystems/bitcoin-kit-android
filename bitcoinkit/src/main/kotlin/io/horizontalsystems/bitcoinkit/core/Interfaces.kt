package io.horizontalsystems.bitcoinkit.core

import io.horizontalsystems.bitcoinkit.models.FeeRate

interface IStorage {
    fun getFeeRate(): FeeRate?
    fun saveFeeRate(feeRate: FeeRate)
}
