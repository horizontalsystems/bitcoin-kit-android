package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.ErrorStorage
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.core.ISyncStateListener
import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.logging.Logger

class InitialSyncer(
        private val storage: IStorage,
        private val blockDiscovery: IBlockDiscovery,
        private val stateManager: StateManager,
        private val publicKeyManager: PublicKeyManager,
        private val stateListener: ISyncStateListener,
        private val errorStorage: ErrorStorage?) {

    interface Listener {
        fun onSyncingFinished()
    }

    var listener: Listener? = null

    private val logger = Logger.getLogger("InitialSyncer")
    private val disposables = CompositeDisposable()
    private var isRestoring = false

    fun sync() {
        if (stateManager.restored) {
            listener?.onSyncingFinished()
            return
        }

        if (isRestoring) return else {
            isRestoring = true
        }

        try {
            stateListener.onSyncStart()
            syncForAccount(0)
        } catch (e: Exception) {
            isRestoring = false
            handle(e)
        }
    }

    fun stop() {
        isRestoring = false
        disposables.clear()
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
                            handle(it)
                        })

        disposables.add(disposable)
    }

    private fun handle(account: Int, keys: List<PublicKey>, blockHashes: List<BlockHash>) {
        publicKeyManager.addKeys(keys)

        if (blockHashes.isNotEmpty()) {
            storage.addBlockHashes(blockHashes)
            syncForAccount(account + 1)
        } else {
            stateManager.restored = true
            listener?.onSyncingFinished()
        }
    }

    private fun handle(error: Throwable) {
        errorStorage?.addApiError(error)
        logger.severe("Initial Sync Error: ${error.message}")

        isRestoring = false
        stateListener.onSyncStop(error)
    }
}
