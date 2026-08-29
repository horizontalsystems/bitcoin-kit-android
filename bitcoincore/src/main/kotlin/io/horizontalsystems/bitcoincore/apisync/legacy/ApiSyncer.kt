package io.horizontalsystems.bitcoincore.apisync.legacy

import io.horizontalsystems.bitcoincore.core.IApiSyncer
import io.horizontalsystems.bitcoincore.core.IApiSyncerListener
import io.horizontalsystems.bitcoincore.core.IPublicKeyManager
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.managers.ApiSyncStateManager
import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.models.PublicKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun terminate() {
        scope.coroutineContext.cancelChildren()
    }

    override fun sync() {
        scope.launch {
            try {
                val (publicKeys, blockHashes) = blockHashDiscovery.discoverBlockHashes()
                val sortedUniqueBlockHashes = blockHashes.distinctBy { it.height }.sortedBy { it.height }

                handle(publicKeys, sortedUniqueBlockHashes)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                handleError(e)
            }
        }
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
