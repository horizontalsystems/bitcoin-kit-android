package io.horizontalsystems.bitcoinkit.storage

import android.content.Context
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.models.BlockchainState
import io.horizontalsystems.bitcoinkit.models.FeeRate

class Storage(context: Context, dbName: String) : IStorage {
    private val store = KitDatabase.getInstance(context, dbName)

    // FeeRate
    override val feeRate: FeeRate?
        get() = store.feeRate().getRate()

    override fun setFeeRate(feeRate: FeeRate) {
        return store.feeRate().insert(feeRate)
    }

    // RestoreState
    override val initialRestored: Boolean?
        get() = store.blockchainState().getState()?.initialRestored

    override fun setInitialRestored(isRestored: Boolean) {
        store.blockchainState().insert(BlockchainState(initialRestored = isRestored))
    }

    override fun clear() {
        store.clearAllTables()
    }
}
