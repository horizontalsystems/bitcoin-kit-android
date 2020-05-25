package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.IApiSyncListener
import io.horizontalsystems.bitcoincore.core.IBlockSyncListener
import io.horizontalsystems.bitcoincore.core.IConnectionManagerListener
import io.horizontalsystems.bitcoincore.core.IKitStateManager
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup
import kotlin.math.max

class SyncManager(
        private val connectionManager: ConnectionManager,
        private val initialSyncer: InitialSyncer,
        private val peerGroup: PeerGroup,
        private val stateManager: IKitStateManager,
        private val apiSyncStateManager: ApiSyncStateManager,
        bestBlockHeight: Int
) : InitialSyncer.Listener, IConnectionManagerListener, IBlockSyncListener, IApiSyncListener {

    private var initialBestBlockHeight = bestBlockHeight
    private var currentBestBlockHeight = bestBlockHeight
    private var foundTransactionsCount = 0

    private fun startSync() {
        if (apiSyncStateManager.restored) {
            startPeerGroup()
        } else {
            startInitialSync()
        }
    }

    private fun startInitialSync() {
        stateManager.setApiSyncStarted()
        initialSyncer.sync()
    }

    private fun startPeerGroup() {
        stateManager.setBlocksSyncStarted()
        peerGroup.start()
    }

    fun start() {
        if (stateManager.syncState !is BitcoinCore.KitState.NotSynced) return

        if (connectionManager.isConnected) {
            startSync()
        } else {
            stateManager.setSyncFailed(BitcoinCore.StateError.NoInternet())
        }
    }

    fun stop() {
        when (stateManager.syncState) {
            is BitcoinCore.KitState.ApiSyncing -> {
                initialSyncer.terminate()
            }
            is BitcoinCore.KitState.Syncing -> {
                peerGroup.stop()
            }
        }
        stateManager.setSyncFailed(BitcoinCore.StateError.NotStarted())
    }

    //
    // ConnectionManager Listener
    //

    override fun onConnectionChange(isConnected: Boolean) {
        if (isConnected && stateManager.syncIdle) {
            startSync()
        } else if (!isConnected && stateManager.syncState is BitcoinCore.KitState.Syncing) {
            peerGroup.stop()
            stateManager.setSyncFailed(BitcoinCore.StateError.NoInternet())
        }
    }

    //
    // InitialSyncer Listener
    //

    override fun onSyncSuccess() {
        apiSyncStateManager.restored = true
        startPeerGroup()
    }

    override fun onSyncFailed(error: Throwable) {
        stateManager.setSyncFailed(error)
    }

    //
    // IApiSyncListener
    //

    override fun onTransactionsFound(count: Int) {
        foundTransactionsCount += count
        stateManager.setApiSyncProgress(foundTransactionsCount)
    }

    //
    // IBlockSyncListener implementations
    //

    override fun onCurrentBestBlockHeightUpdate(height: Int, maxBlockHeight: Int) {
        if (!connectionManager.isConnected) return

        currentBestBlockHeight = max(currentBestBlockHeight, height)

        val blocksDownloaded = currentBestBlockHeight - initialBestBlockHeight
        val allBlocksToDownload = maxBlockHeight - initialBestBlockHeight

        val progress = when {
            allBlocksToDownload <= 0 -> 1.0
            else -> blocksDownloaded / allBlocksToDownload.toDouble()
        }

        if (progress >= 1) {
            stateManager.setSyncFinished()
        } else {
            stateManager.setBlocksSyncProgress(progress)
        }
    }

    override fun onBlockSyncFinished() {
        stateManager.setSyncFinished()
    }
}
