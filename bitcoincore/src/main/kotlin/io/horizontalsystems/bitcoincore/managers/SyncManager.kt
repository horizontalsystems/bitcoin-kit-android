package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.IConnectionManagerListener
import io.horizontalsystems.bitcoincore.core.ISyncStateListener
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup

class SyncManager(private val peerGroup: PeerGroup,
                  private val initialSyncer: InitialSyncer,
                  private val stateListener: ISyncStateListener)
    : InitialSyncer.Listener, IConnectionManagerListener {

    fun start() {
        stateListener.onSyncStart()
        initialSyncer.sync()
    }

    fun stop(error: Exception) {
        initialSyncer.stop()
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
        initialSyncer.stop()
        peerGroup.start()
    }

    override fun onSyncFailed(error: Throwable) {
        stateListener.onSyncStop(error)
    }
}
