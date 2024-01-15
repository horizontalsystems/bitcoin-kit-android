package io.horizontalsystems.bitcoincore.apisync.legacy

import io.horizontalsystems.bitcoincore.core.IApiSyncer
import io.horizontalsystems.bitcoincore.core.IApiSyncerListener
import io.horizontalsystems.bitcoincore.core.IPublicKeyManager
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.managers.ApiSyncStateManager
import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.logging.Logger

class ApiSyncer(
    private val storage: IStorage,
    private val blockHashDiscovery: BlockHashDiscoveryBatch,
    private val publicKeyManager: IPublicKeyManager,
    private val multiAccountPublicKeyFetcher: IMultiAccountPublicKeyFetcher?,
    private val apiSyncStateManager: ApiSyncStateManager
) : IApiSyncer {

    override val willSync: Boolean
        get() = !apiSyncStateManager.restored

    override var listener: IApiSyncerListener? = null

    private val logger = Logger.getLogger("ApiSyncer")
    private val disposables = CompositeDisposable()

    override fun terminate() {
        disposables.clear()
    }

    override fun sync() {
        val disposable = blockHashDiscovery.discoverBlockHashes()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
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
        apiSyncStateManager.restored = true
        listener?.onSyncSuccess()
    }

    private fun handleError(error: Throwable) {
        logger.severe("Initial Sync Error: ${error.message}")

        listener?.onSyncFailed(error)
    }
}
