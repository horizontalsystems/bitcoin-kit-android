package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCore.KitState

class KitStateManager : IKitStateManager {

    //
    // IKitStateManager
    //
    override var syncState: KitState = KitState.NotSynced(BitcoinCore.StateError.NotStarted())
        private set(value) {
            if (value != field) {
                field = value
                listener?.onKitStateUpdate(field)
            }
        }

    override val syncIdle: Boolean
        get() = syncState.let {
            it is KitState.NotSynced && it.exception !is BitcoinCore.StateError.NotStarted
        }

    override var listener: IKitStateManagerListener? = null

    override fun setApiSyncStarted() {
        syncState = KitState.ApiSyncing(0)
    }

    override fun setApiSyncProgress(foundTransactionsCount: Int) {
        syncState = KitState.ApiSyncing(foundTransactionsCount)
    }

    override fun setBlocksSyncStarted() {
        syncState = KitState.Syncing(0.0)
    }

    override fun setBlocksSyncProgress(progress: Double) {
        syncState = KitState.Syncing(progress)
    }

    override fun setSyncFailed(error: Throwable) {
        syncState = KitState.NotSynced(error)
    }

    override fun setSyncFinished() {
        syncState = KitState.Synced
    }
}
