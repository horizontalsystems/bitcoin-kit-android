package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.IPublicKeyManager
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.logging.Logger

class InitialSyncer(
    private val storage: IStorage,
    private val blockDiscovery: IBlockDiscovery,
    private val publicKeyManager: IPublicKeyManager,
    private val multiAccountPublicKeyFetcher: IMultiAccountPublicKeyFetcher?
) {

    interface Listener {
        fun onSyncSuccess()
        fun onSyncFailed(error: Throwable)
    }

    var listener: Listener? = null

    private val logger = Logger.getLogger("InitialSyncer")
    private val disposables = CompositeDisposable()

    fun terminate() {
        disposables.clear()
    }

    fun sync() {
        val disposable = blockDiscovery.discoverBlockHashes()
            .subscribeOn(Schedulers.io())
            .subscribe(
                { (publicKeys, blockHashes) ->
                    val sortedUniqueBlockHashes = blockHashes.distinctBy { it.height }.sortedBy { it.height }

                    handle(publicKeys, sortedUniqueBlockHashes)
                },
                {
                    handleError(it)
                })

        disposables.add(disposable)
    }

    private fun handle(keys: List<PublicKey>, blockHashes: List<BlockHash>) {
        publicKeyManager.addKeys(keys)

        if (multiAccountPublicKeyFetcher != null) {
            if (blockHashes.isNotEmpty()) {
                storage.addBlockHashes(blockHashes)
                multiAccountPublicKeyFetcher.increaseAccount()
                sync()
            } else {
                handleSuccess()
            }
        } else {
            storage.addBlockHashes(blockHashes)
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
