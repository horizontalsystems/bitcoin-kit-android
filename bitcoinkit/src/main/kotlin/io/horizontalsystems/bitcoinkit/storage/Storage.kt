package io.horizontalsystems.bitcoinkit.storage

import android.content.Context
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.models.FeeRate

class Storage(context: Context, dbName: String) : IStorage {
    private val store = KitDatabase.getInstance(context, dbName)

    override fun getFeeRate(): FeeRate? {
        return store.feeRateDao().getRate()
    }

    override fun saveFeeRate(feeRate: FeeRate) {
        return store.feeRateDao().insert(feeRate)
    }
}
