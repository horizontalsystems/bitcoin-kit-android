package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.IConnectionManagerListener
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup

class SyncManager(private val peerGroup: PeerGroup, private val initialSyncer: InitialSyncer)
    : InitialSyncer.Listener, IConnectionManagerListener {

    fun start() {
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

    override fun onSyncingFinished() {
        initialSyncer.stop()
        peerGroup.start()
    }
}
