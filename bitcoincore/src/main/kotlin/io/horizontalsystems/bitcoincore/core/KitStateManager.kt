package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCore.KitState
import kotlin.math.max

class KitStateManager : IKitStateManager, IBlockSyncListener, IApiSyncListener {

    private var initialBestBlockHeight = 0
    private var currentBestBlockHeight = 0
    private var foundTransactionsCount = 0

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
        syncState = KitState.ApiSyncing(foundTransactionsCount)
    }

    override fun setBlocksSyncStarted() {
        syncState = KitState.Syncing(0.0)
    }

    override fun setSyncFailed(error: Throwable) {
        syncState = KitState.NotSynced(error)
    }

    //
    // IApiSyncListener
    //

    override fun onTransactionsFound(count: Int) {
        foundTransactionsCount += count
        syncState = KitState.ApiSyncing(foundTransactionsCount)
    }

    //
    // IBlockSyncListener implementations
    //

    override fun onInitialBestBlockHeightUpdate(height: Int) {
        initialBestBlockHeight = height
        currentBestBlockHeight = height
    }

    override fun onCurrentBestBlockHeightUpdate(height: Int, maxBlockHeight: Int) {
        currentBestBlockHeight = max(currentBestBlockHeight, height)

        val blocksDownloaded = currentBestBlockHeight - initialBestBlockHeight
        val allBlocksToDownload = maxBlockHeight - initialBestBlockHeight

        val progress = when {
            allBlocksToDownload <= 0 -> 1.0
            else -> blocksDownloaded / allBlocksToDownload.toDouble()
        }

        syncState = if (progress >= 1) {
            KitState.Synced
        } else {
            KitState.Syncing(progress)
        }
    }

    override fun onBlockSyncFinished() {
        syncState = KitState.Synced
    }

}
