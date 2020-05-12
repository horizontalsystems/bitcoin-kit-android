package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.IConnectionManagerListener
import io.horizontalsystems.bitcoincore.core.ISyncStateListener
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup

class SyncManager(private val peerGroup: PeerGroup,
                  private val initialSyncer: InitialSyncer,
                  private val stateListener: ISyncStateListener,
                  private val stateManager: StateManager)
    : InitialSyncer.Listener, IConnectionManagerListener {

    fun start() {
        stateListener.onSyncStart()

        if (stateManager.restored) {
            peerGroup.start()
        } else {
            initialSyncer.sync()
        }
    }

    fun stop(error: Exception) {
        initialSyncer.terminate()
        peerGroup.stop(error)
    }

    //
    // ConnectionManager Listener
    //

    override fun onConnectionChange(isConnected: Boolean) {
        if (isConnected) {
            start()
        } else {
            stop(BitcoinCore.StateError.NoInternet())
        }
    }

    //
    // InitialSyncer Listener
    //

    override fun onSyncSuccess() {
        stateManager.restored = true
        peerGroup.start()
    }

    override fun onSyncFailed(error: Throwable) {
        stateListener.onSyncStop(error)
    }
}
