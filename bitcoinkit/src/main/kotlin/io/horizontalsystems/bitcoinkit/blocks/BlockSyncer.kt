package io.horizontalsystems.bitcoinkit.blocks

import io.horizontalsystems.bitcoinkit.core.ISyncStateListener
import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.managers.AddressManager
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.bitcoinkit.transactions.TransactionProcessor
import io.realm.Sort

class BlockSyncer(private val realmFactory: RealmFactory,
                  private val blockchain: Blockchain,
                  private val transactionProcessor: TransactionProcessor,
                  private val addressManager: AddressManager,
                  private val bloomFilterManager: BloomFilterManager,
                  private val listener: ISyncStateListener,
                  private val network: Network) {

    val localDownloadedBestBlockHeight: Int?
        get() = realmFactory.realm.use {
            it.where(Block::class.java).sort("height", Sort.DESCENDING).findFirst()?.height
        }

    val localKnownBestBlockHeight: Int
        get() = realmFactory.realm.use { realm ->
            val blockHashesToDownload = realm.where(BlockHash::class.java).findAll().map { it.reversedHeaderHashHex }
            val alreadyDownloadedBlockHashesCount = realm.where(Block::class.java).`in`("reversedHeaderHashHex", blockHashesToDownload.toTypedArray()).count()

            val newBlockHashesCount = realm.where(BlockHash::class.java).equalTo("height", 0 as Int).count().minus(alreadyDownloadedBlockHashesCount).toInt()

            return (localDownloadedBestBlockHeight ?: 0).plus(newBlockHashesCount)
        }

    private var needToRedownload = false

    init {
        val realm = realmFactory.realm

        if (realm.where(Block::class.java).count() == 0L) {
            try {
                realm.executeTransaction {
                    realm.insert(network.checkpointBlock)
                }
            } catch (e: RuntimeException) {
            }
        }

        listener.onInitialBestBlockHeightUpdate(localDownloadedBestBlockHeight ?: 0)

        realm.close()
    }

    fun prepareForDownload() {
        needToRedownload = false
        addressManager.fillGap()

        clearNotFullBlocks()
        clearBlockHashes()

        handleFork()

        bloomFilterManager.regenerateBloomFilter()
    }

    fun downloadStarted() {
    }

    fun downloadIterationCompleted() {
        if (needToRedownload) {
            needToRedownload = false
            addressManager.fillGap()
            bloomFilterManager.regenerateBloomFilter()
        }
    }

    fun downloadCompleted() {
        handleFork()
    }

    fun downloadFailed() {
        prepareForDownload()
    }

    private fun handleFork() {
        realmFactory.realm.use {
            blockchain.handleFork(it)
        }
    }

    fun getBlockHashes(): List<BlockHash> {
        realmFactory.realm.use { realm ->
            val blockHashes = realm.where(BlockHash::class.java)
                    .sort("order", Sort.ASCENDING, "height", Sort.ASCENDING)
                    .findAll()

            return blockHashes.take(500).map { realm.copyFromRealm(it) }
        }
    }

    // we need to clear block hashes when "syncPeer" is disconnected
    private fun clearBlockHashes() {
        realmFactory.realm.use { realm ->
            realm.executeTransaction {
                realm.where(BlockHash::class.java)
                        // block hashes except ones taken from API
                        .equalTo("height", 0L)
                        .findAll()
                        .deleteAllFromRealm()
            }
        }
    }

    fun getBlockLocatorHashes(peerLastBlockHeight: Int): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        val realm = realmFactory.realm

        realm.where(BlockHash::class.java)
                .equalTo("height", 0L)
                .sort("order", Sort.DESCENDING)
                .findFirst()
                ?.headerHash
                ?.let {
                    result.add(it)
                }

        if (result.isEmpty()) {
            realm.where(Block::class.java)
                    .greaterThan("height", network.checkpointBlock.height)
                    .sort("height", Sort.DESCENDING)
                    .findAll()
                    .take(10)
                    .forEach {
                        result.add(it.headerHash)
                    }
        }

        val checkPointHeaderHash = (realm.where(Block::class.java)
                .equalTo("height", peerLastBlockHeight)
                .findFirst() ?: network.checkpointBlock).headerHash

        result.add(checkPointHeaderHash)

        realm.close()

        return result
    }

    fun addBlockHashes(blockHashes: List<ByteArray>) {
        val realm = realmFactory.realm

        var lastOrder = realm.where(BlockHash::class.java)
                .sort("order", Sort.DESCENDING)
                .findFirst()
                ?.order ?: 0

        realm.executeTransaction {
            blockHashes.forEach { hash ->
                val blockHash = realm.where(BlockHash::class.java).equalTo("headerHash", hash).findFirst()
                if (blockHash == null) {
                    realm.insert(BlockHash(hash, 0, ++lastOrder))
                }
            }
        }

        realm.close()
    }

    fun handleMerkleBlock(merkleBlock: MerkleBlock, maxBlockHeight: Int) {
        val realm = realmFactory.realm

        realm.executeTransaction {
            val height = merkleBlock.height

            val block = when (height) {
                null -> blockchain.connect(merkleBlock, realm)
                else -> blockchain.forceAdd(merkleBlock, height, realm)
            }

            try {
                transactionProcessor.process(merkleBlock.associatedTransactions, block, needToRedownload, realm)
            } catch (e: BloomFilterManager.BloomFilterExpired) {
                needToRedownload = true
            }

            if (!needToRedownload) {
                realm.where(BlockHash::class.java)
                        .equalTo("reversedHeaderHashHex", block.reversedHeaderHashHex)
                        .findFirst()
                        ?.deleteFromRealm()
            }

            listener.onCurrentBestBlockHeightUpdate(block.height, maxBlockHeight)
        }

        realm.close()
    }

    private fun clearNotFullBlocks() {
        val realm = realmFactory.realm

        val toDelete = realm.where(BlockHash::class.java)
                .equalTo("height", 0L)
                .notEqualTo("reversedHeaderHashHex", network.checkpointBlock.reversedHeaderHashHex)
                .findAll().map { it.reversedHeaderHashHex }.toTypedArray()

        realm.executeTransaction {
            blockchain.deleteBlocks(realm.where(Block::class.java).`in`("reversedHeaderHashHex", toDelete).findAll())
        }

        realm.close()
    }

    fun shouldRequest(blockHash: ByteArray): Boolean {
        val realm = realmFactory.realm

        val blockExist = realm.where(Block::class.java)
                .equalTo("headerHash", blockHash)
                .count() > 0

        realm.close()

        return !blockExist
    }

}
