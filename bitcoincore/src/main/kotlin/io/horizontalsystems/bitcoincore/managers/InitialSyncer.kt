package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.logging.Logger

class InitialSyncer(
        private val storage: IStorage,
        private val blockDiscovery: IBlockDiscovery,
        private val publicKeyManager: PublicKeyManager
) {

    interface Listener {
        fun onSyncSuccess()
        fun onSyncFailed(error: Throwable)
    }

    var listener: Listener? = null

    private val logger = Logger.getLogger("InitialSyncer")
    private val disposables = CompositeDisposable()

    fun sync() {
        syncForAccount(0)
    }

    fun terminate() {
        disposables.clear()
    }

    private fun syncForAccount(account: Int) {
        val disposable = blockDiscovery.discoverBlockHashes(account)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { (publicKeys, blockHashes) ->
                            val sortedUniqueBlockHashes = blockHashes.distinct().sortedBy { it.height }

                            handle(account, publicKeys, sortedUniqueBlockHashes)
                        },
                        {
                            handleError(it)
                        })

        disposables.add(disposable)
    }

    private fun handle(account: Int, keys: List<PublicKey>, blockHashes: List<BlockHash>) {
        publicKeyManager.addKeys(keys)

        if (blockHashes.isNotEmpty()) {
            storage.addBlockHashes(blockHashes)
            syncForAccount(account + 1)
        } else {
            handleSuccess()
        }
    }

    private fun handleSuccess() {
        listener?.onSyncSuccess()
    }

    private fun handleError(error: Throwable) {
        logger.severe("Initial Sync Error: ${error.message}")

        listener?.onSyncFailed(error)
    }
}
