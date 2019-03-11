package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.core.ISyncStateListener
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.network.peer.PeerGroup
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.logging.Logger

class InitialSyncer(
        private val storage: IStorage,
        private val blockDiscovery: IBlockDiscovery,
        private val stateManager: StateManager,
        private val addressManager: AddressManager,
        private val peerGroup: PeerGroup,
        private val listener: ISyncStateListener) {

    private val logger = Logger.getLogger("InitialSyncer")
    private val disposables = CompositeDisposable()
    private var isRunning = false

    @Throws
    fun sync() {
        if (isRunning) return

        isRunning = true

        addressManager.fillGap()

        try {
            if (stateManager.restored) {
                peerGroup.start()
            } else {
                listener.onSyncStart()

                syncForAccount(0)
            }
        } catch (e: Exception) {
            isRunning = false
            throw e
        }
    }

    private fun syncForAccount(account: Int) {
        val externalObservable = blockDiscovery.discoverBlockHashes(account, true)
        val internalObservable = blockDiscovery.discoverBlockHashes(account, false)

        val disposable = Single
                .merge(externalObservable, internalObservable)
                .toList()
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { pairsList ->
                            val publicKeys = mutableListOf<PublicKey>()
                            val blockHashes = mutableListOf<BlockHash>()
                            pairsList.forEach { (keys, hashes) ->
                                publicKeys.addAll(keys)
                                blockHashes.addAll(hashes)
                            }
                            handle(account, publicKeys, blockHashes)
                        },
                        {
                            isRunning = false
                            logger.severe("Initial Sync Error: $it")
                            listener.onSyncStop()
                        })

        disposables.add(disposable)
    }

    fun stop() {
        peerGroup.close()
        disposables.clear()
    }

    @Throws
    private fun handle(account: Int, keys: List<PublicKey>, blockHashes: List<BlockHash>) {
        addressManager.addKeys(keys)

        if (blockHashes.isNotEmpty()) {
            storage.addBlockHashes(blockHashes)
            syncForAccount(account + 1)
        } else {
            stateManager.restored = true
            peerGroup.start()
        }
    }

}
