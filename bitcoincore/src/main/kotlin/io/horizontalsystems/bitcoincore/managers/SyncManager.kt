package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.IConnectionManagerListener
import io.horizontalsystems.bitcoincore.core.ISyncStateListener
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup

class SyncManager(private val peerGroup: PeerGroup,
                  private val initialSyncer: InitialSyncer,
                  private val syncStateListener: ISyncStateListener,
                  private val stateManager: StateManager,
                  private val connectionManager: ConnectionManager)
    : InitialSyncer.Listener, IConnectionManagerListener {

    sealed class State {
        object Stopped : State()
        object Idle : State()
        object InitialSyncing : State()
        object PeerGroupRunning : State()
    }

    private var state: State = State.Stopped

    fun start() {
        if (state !is State.Stopped && state !is State.Idle) return

        if (connectionManager.isConnected) {
            startSync()
        } else {
            state = State.Idle
            syncStateListener.onSyncStop(BitcoinCore.StateError.NoInternet())
        }
    }

    private fun startSync() {
        syncStateListener.onSyncStart()

        if (stateManager.restored) {
            startPeerGroup()
        } else {
            state = State.InitialSyncing
            initialSyncer.sync()
        }
    }

    private fun startPeerGroup() {
        state = State.PeerGroupRunning
        peerGroup.start()
    }

    fun stop() {
        when (state) {
            State.InitialSyncing -> {
                initialSyncer.terminate()
            }
            State.PeerGroupRunning -> {
                peerGroup.stop()
            }
        }

        state = State.Stopped
        syncStateListener.onSyncStop(BitcoinCore.StateError.NotStarted())
    }

    //
    // ConnectionManager Listener
    //

    override fun onConnectionChange(isConnected: Boolean) {
        if (isConnected && state is State.Idle) {
            startSync()
        } else if (!isConnected && state == State.PeerGroupRunning) {
            peerGroup.stop()
            state = State.Idle
            syncStateListener.onSyncStop(BitcoinCore.StateError.NoInternet())
        }
    }

    //
    // InitialSyncer Listener
    //

    override fun onSyncSuccess() {
        stateManager.restored = true
        startPeerGroup()
    }

    override fun onSyncFailed(error: Throwable) {
        state = State.Idle
        syncStateListener.onSyncStop(error)
    }
}
