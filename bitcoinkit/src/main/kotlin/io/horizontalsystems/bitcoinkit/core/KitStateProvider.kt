package io.horizontalsystems.bitcoinkit.core

import io.horizontalsystems.bitcoinkit.BitcoinKit.KitState

interface ISyncStateListener {
    fun onSyncStart()
    fun onSyncStop()
    fun onInitialBestBlockHeight(height: Int)
    fun onCurrentBestBlockHeight(height: Int, maxBlockHeight: Int)
}

class KitStateProvider(private val listener: Listener) : ISyncStateListener {

    interface Listener {
        fun onKitStateUpdate(state: KitState)
    }

    private var initialBestBlockHeight = 0
    private var currentBestBlockHeight = 0

    //
    // SyncStateListener implementations
    //
    override fun onSyncStart() {
        listener.onKitStateUpdate(KitState.Syncing(0.0))
    }

    override fun onSyncStop() {
        listener.onKitStateUpdate(KitState.NotSynced)
    }

    override fun onInitialBestBlockHeight(height: Int) {
        initialBestBlockHeight = height
        currentBestBlockHeight = height
    }

    override fun onCurrentBestBlockHeight(height: Int, maxBlockHeight: Int) {
        currentBestBlockHeight = Math.max(currentBestBlockHeight, height)

        val blocksDownloaded = currentBestBlockHeight - initialBestBlockHeight
        val allBlocksToDownload = maxBlockHeight - initialBestBlockHeight

        val progress = when {
            allBlocksToDownload <= 0 -> 1.0
            else -> blocksDownloaded / allBlocksToDownload.toDouble()
        }

        if (progress >= 1) {
            listener.onKitStateUpdate(KitState.Synced)
        } else {
            listener.onKitStateUpdate(KitState.Syncing(progress))
        }

    }
}
