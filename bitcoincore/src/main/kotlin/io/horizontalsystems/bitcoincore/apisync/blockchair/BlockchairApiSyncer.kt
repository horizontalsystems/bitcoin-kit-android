package io.horizontalsystems.bitcoincore.apisync.blockchair

import io.horizontalsystems.bitcoincore.blocks.Blockchain
import io.horizontalsystems.bitcoincore.core.IApiSyncer
import io.horizontalsystems.bitcoincore.core.IApiSyncerListener
import io.horizontalsystems.bitcoincore.core.IApiTransactionProvider
import io.horizontalsystems.bitcoincore.core.IPublicKeyManager
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.extensions.toReversedByteArray
import io.horizontalsystems.bitcoincore.managers.ApiSyncStateManager
import io.horizontalsystems.bitcoincore.managers.IRestoreKeyConverter
import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.models.BlockHashPublicKey
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.logging.Logger

class BlockchairApiSyncer(
    private val storage: IStorage,
    private val restoreKeyConverter: IRestoreKeyConverter,
    private val transactionProvider: IApiTransactionProvider,
    private val lastBlockProvider: BlockchairLastBlockProvider,
    private val publicKeyManager: IPublicKeyManager,
    private val blockchain: Blockchain,
    private val apiSyncStateManager: ApiSyncStateManager,
) : IApiSyncer {

    private val logger = Logger.getLogger("BlockchairApiSyncer")
    private val disposables = CompositeDisposable()

    // A retry can arrive while a previous scan is still running (connection flaps,
    // manual refresh); two interleaved scans write discovery state concurrently and
    // finish in arbitrary order, so only one scan may run at a time.
    @Volatile
    private var syncing = false

    override var listener: IApiSyncerListener? = null

    override val willSync: Boolean = true

    override fun sync() {
        if (syncing) return
        syncing = true

        scanSingle()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .doFinally { syncing = false }
            .subscribe({}, {
                handleError(it)
            }).let {
                disposables.add(it)
            }
    }

    override fun terminate() {
        disposables.clear()
        syncing = false
    }

    private fun handleError(error: Throwable) {
        logger.severe("Error: ${error.message}")
        listener?.onSyncFailed(error)
    }

    private fun fetchLastBlock() {
        val blockHeaderItem = lastBlockProvider.lastBlockHeader()
        val header = BlockHeader(
            version = 0,
            hash = blockHeaderItem.hash,
            previousBlockHeaderHash = byteArrayOf(),
            merkleRoot = byteArrayOf(),
            timestamp = blockHeaderItem.timestamp,
            bits = 0,
            nonce = 0
        )

        blockchain.insertLastBlock(header, blockHeaderItem.height)
    }

    private fun scanSingle(): Single<Unit> = Single.create { emitter ->
        try {
            val allKeys = storage.getPublicKeys()
            // A height-based cutoff is only valid for incremental re-scans of a wallet
            // whose initial restore has completed. During the initial restore the storage
            // may already hold block hashes from an earlier failed attempt; using their
            // height as a cutoff would skip all older history for keys discovered later
            // (gap expansion), permanently losing transactions.
            val stopHeight = if (apiSyncStateManager.restored) {
                storage.downloadedTransactionsBestBlockHeight()
            } else {
                null
            }
            fetchRecursive(allKeys, allKeys, stopHeight)
            fetchLastBlock()

            apiSyncStateManager.restored = true
            listener?.onSyncSuccess()
            emitter.onSuccess(Unit)
        } catch (e: Throwable) {
            emitter.tryOnError(e)
        }
    }

    private fun fetchRecursive(
        keys: List<PublicKey>,
        allKeys: List<PublicKey>,
        stopHeight: Int?
    ) {
        val publicKeyMap = mutableMapOf<String, PublicKey>()
        val addresses = mutableListOf<String>()

        for (key in keys) {
            val restoreKeys = restoreKeyConverter.keysForApiRestore(key)
            for (address in restoreKeys) {
                addresses.add(address)
                publicKeyMap[address] = key
            }
        }

        val transactionItems = transactionProvider.transactions(addresses, stopHeight)
        val blockHashes = mutableListOf<BlockHash>()
        val blockHashPublicKeys = mutableListOf<BlockHashPublicKey>()

        for (transactionItem in transactionItems) {
            val hash = transactionItem.blockHash.toReversedByteArray()

            if (blockHashes.none { it.headerHash.contentEquals(hash) }) {
                BlockHash(hash, transactionItem.blockHeight).also {
                    blockHashes.add(it)
                }
            }

            transactionItem.addressItems.forEach { addressItem ->
                val publicKey = publicKeyMap[addressItem.address] ?: publicKeyMap[addressItem.script]
                if (publicKey != null) {
                    blockHashPublicKeys.add(BlockHashPublicKey(hash, publicKey.path))
                }
            }
        }

        storage.addBlockHashes(blockHashes)
        storage.addBockHashPublicKeys(blockHashPublicKeys)
        listener?.onTransactionsFound(transactionItems.size)

        publicKeyManager.fillGap()

        val _allKeys = storage.getPublicKeys()
        val newKeys = _allKeys.minus(allKeys.toSet())

        if (newKeys.isNotEmpty()) {
            fetchRecursive(newKeys, _allKeys, stopHeight)
        }
    }
}
