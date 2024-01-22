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

    override var listener: IApiSyncerListener? = null

    override val willSync: Boolean = true

    override fun sync() {
        scanSingle()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe({}, {
                handleError(it)
            }).let {
                disposables.add(it)
            }
    }

    override fun terminate() {
        disposables.clear()
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

    private fun scanSingle(): Single<Unit> = Single.create {
        val allKeys = storage.getPublicKeys()
        val stopHeight = storage.downloadedTransactionsBestBlockHeight()
        fetchRecursive(allKeys, allKeys, stopHeight)
        fetchLastBlock()

        apiSyncStateManager.restored = true
        listener?.onSyncSuccess()
    }

    private fun fetchRecursive(
        keys: List<PublicKey>,
        allKeys: List<PublicKey>,
        stopHeight: Int
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
