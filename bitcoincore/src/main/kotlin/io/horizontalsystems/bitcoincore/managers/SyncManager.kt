package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCore.KitState
import io.horizontalsystems.bitcoincore.apisync.legacy.ApiSyncer
import io.horizontalsystems.bitcoincore.core.*
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup
import kotlin.math.max

class SyncManager(
    private val connectionManager: ConnectionManager,
    private val apiSyncer: IApiSyncer,
    private val peerGroup: PeerGroup,
    private val apiSyncStateManager: ApiSyncStateManager,
    bestBlockHeight: Int
) : IApiSyncerListener, IConnectionManagerListener, IBlockSyncListener {

    var listener: IKitStateListener? = null

    var syncState: KitState = KitState.NotSynced(BitcoinCore.StateError.NotStarted())
        private set(value) {
            if (value != field) {
                field = value
                
                listener?.onKitStateUpdate(field)
            }
        }

    private val syncIdle: Boolean
        get() = syncState.let {
            it is KitState.NotSynced && it.exception !is BitcoinCore.StateError.NotStarted
        }

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
        syncState = KitState.ApiSyncing(0)
        apiSyncer.sync()
    }

    private fun startPeerGroup() {
        syncState = KitState.Syncing(0.0)
        peerGroup.start()
    }

    fun start() {
        if (syncState !is KitState.NotSynced) return

        if (connectionManager.isConnected) {
            startSync()
        } else {
            syncState = KitState.NotSynced(BitcoinCore.StateError.NoInternet())
        }
    }

    fun stop() {
        when (syncState) {
            is KitState.ApiSyncing -> {
                apiSyncer.terminate()
            }
            is KitState.Syncing, is KitState.Synced -> {
                peerGroup.stop()
            }
            else -> Unit
        }
        syncState = KitState.NotSynced(BitcoinCore.StateError.NotStarted())
    }

    //
    // ConnectionManager Listener
    //

    override fun onConnectionChange(isConnected: Boolean) {
        if (isConnected && syncIdle) {
            startSync()
        } else if (!isConnected && (syncState is KitState.Syncing || syncState is KitState.Synced)) {
            peerGroup.stop()
            syncState = KitState.NotSynced(BitcoinCore.StateError.NoInternet())
        }
    }

    //
    // IApiSyncerListener
    //

    override fun onSyncSuccess() {
        startPeerGroup()
    }

    override fun onSyncFailed(error: Throwable) {
        syncState = KitState.NotSynced(error)
    }

    override fun onTransactionsFound(count: Int) {
        foundTransactionsCount += count
        syncState = KitState.ApiSyncing(foundTransactionsCount)
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
