package io.horizontalsystems.bitcoinkit.core

import io.horizontalsystems.bitcoinkit.BitcoinCore.KitState

interface ISyncStateListener {
    fun onSyncStart()
    fun onSyncStop()
    fun onSyncFinish()
    fun onInitialBestBlockHeightUpdate(height: Int)
    fun onCurrentBestBlockHeightUpdate(height: Int, maxBlockHeight: Int)
}

class KitStateProvider : ISyncStateListener {

    interface Listener {
        fun onKitStateUpdate(state: KitState)
    }

    var listener: Listener? = null
    private var initialBestBlockHeight = 0
    private var currentBestBlockHeight = 0

    var syncState: KitState = KitState.NotSynced
        private set(value) {
            if (value != field) {
                field = value
                listener?.onKitStateUpdate(field)
            }
        }

    //
    // SyncStateListener implementations
    //
    override fun onSyncStart() {
        syncState = KitState.Syncing(0.0)
    }

    override fun onSyncStop() {
        syncState = KitState.NotSynced
    }

    override fun onSyncFinish() {
        syncState = KitState.Synced
    }

    override fun onInitialBestBlockHeightUpdate(height: Int) {
        initialBestBlockHeight = height
        currentBestBlockHeight = height
    }

    override fun onCurrentBestBlockHeightUpdate(height: Int, maxBlockHeight: Int) {
        currentBestBlockHeight = Math.max(currentBestBlockHeight, height)

        val blocksDownloaded = currentBestBlockHeight - initialBestBlockHeight
        val allBlocksToDownload = maxBlockHeight - initialBestBlockHeight

        val progress = when {
            allBlocksToDownload <= 0 -> 1.0
            else -> blocksDownloaded / allBlocksToDownload.toDouble()
        }

        if (progress >= 1) {
            syncState = KitState.Synced
        } else {
            syncState = KitState.Syncing(progress)
        }

    }
}
