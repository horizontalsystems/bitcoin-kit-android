package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.network.peer.PeerGroup

class SyncManager(private val peerGroup: PeerGroup, private val initialSyncer: InitialSyncer) : InitialSyncer.Listener {

    fun start() {
        initialSyncer.sync()
    }

    fun stop() {
        peerGroup.close()

        initialSyncer.stop()
    }

    //
    // InitialSyncer Listener
    //

    override fun onSyncingFinished() {
        if (!peerGroup.isAlive) {
            peerGroup.start()
        }
    }
}
