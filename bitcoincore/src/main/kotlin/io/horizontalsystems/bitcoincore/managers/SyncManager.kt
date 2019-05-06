package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.network.peer.PeerGroup
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

class SyncManager(
        private val connectionManager: ConnectionManager,
        private val feeRateSyncer: FeeRateSyncer,
        private val peerGroup: PeerGroup,
        private val initialSyncer: InitialSyncer,
        private val timer: Observable<Long> = Observable.interval(0, 3, TimeUnit.MINUTES))
    : InitialSyncer.Listener {

    private var disposable: Disposable? = null

    fun start() {
        initialSyncer.sync()

        if (disposable != null && disposable?.isDisposed == false) {
            return
        }

        disposable = timer.subscribe {
            syncFeeRate()
        }
    }

    fun stop() {
        peerGroup.close()

        initialSyncer.stop()

        disposable?.dispose()
        disposable = null
    }

    //
    // InitialSyncer Listener
    //

    override fun onSyncingFinished() {
        if (!peerGroup.isAlive) {
            peerGroup.start()
        }
    }

    private fun syncFeeRate() {
        if (connectionManager.isOnline) {
            feeRateSyncer.sync()
        }
    }
}
