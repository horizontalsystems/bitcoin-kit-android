package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.IConnectionManagerListener
import io.horizontalsystems.bitcoincore.core.IKitStateManager
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup

class SyncManager(
        private val connectionManager: ConnectionManager,
        private val initialSyncer: InitialSyncer,
        private val peerGroup: PeerGroup,
        private val stateManager: IKitStateManager,
        private val apiSyncStateManager: ApiSyncStateManager
) : InitialSyncer.Listener, IConnectionManagerListener {

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
}
