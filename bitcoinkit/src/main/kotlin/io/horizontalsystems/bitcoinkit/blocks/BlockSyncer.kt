package io.horizontalsystems.bitcoinkit.blocks

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.managers.AddressManager
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.network.NetworkParameters
import io.horizontalsystems.bitcoinkit.transactions.TransactionProcessor
import io.realm.Sort

class BlockSyncer(private val realmFactory: RealmFactory,
                  private val blockchain: Blockchain,
                  private val transactionProcessor: TransactionProcessor,
                  private val addressManager: AddressManager,
                  private val bloomFilterManager: BloomFilterManager,
                  private val network: NetworkParameters) {

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
        needToRedownload = false
        addressManager.fillGap()
        bloomFilterManager.regenerateBloomFilter()
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

    fun getBlockHashes(): List<ByteArray> {
        val realm = realmFactory.realm
        val blockHashes = realm.where(BlockHash::class.java)
                .sort("order")
                .findAll()

        val result = blockHashes.take(500).map { it.headerHash }
        realm.close()

        return result
    }

    // we need to clear block hashes when sync peer is disconnected
    private fun clearBlockHashes() {
        val realm = realmFactory.realm

        realm.executeTransaction {
            realm.where(BlockHash::class.java)
                    // block hashes except ones taken from API
                    .equalTo("height", 0L)
                    .findAll()
                    .deleteAllFromRealm()
        }

        realm.close()
    }

    fun getBlockLocatorHashes(): List<ByteArray> {
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
                    .sort("height", Sort.DESCENDING)
                    .findAll()
                    .take(10)
                    .forEach {
                        result.add(it.headerHash)
                    }
        }

        result.add(network.checkpointBlock.headerHash)

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
                realm.insert(BlockHash(hash, 0, ++lastOrder))
            }
        }

        realm.close()
    }

    fun handleMerkleBlock(merkleBlock: MerkleBlock) {
        val realm = realmFactory.realm

        realm.executeTransaction {
            val block = blockchain.connect(merkleBlock, realm)

            try {
                transactionProcessor.process(merkleBlock.associatedTransactions, block, !needToRedownload, realm)
            } catch (e: BloomFilterManager.BloomFilterExpired) {
                needToRedownload = true
            }

            if (!needToRedownload) {
                realm.where(BlockHash::class.java)
                        .equalTo("reversedHeaderHashHex", block.reversedHeaderHashHex)
                        .findFirst()
                        ?.deleteFromRealm()
            }
        }

        realm.close()
    }

    private fun clearNotFullBlocks() {
        val realm = realmFactory.realm

        val toDelete = realm.where(BlockHash::class.java).findAll().map { it.reversedHeaderHashHex }.toTypedArray()

        realm.executeTransaction {
            realm.where(Block::class.java).`in`("reversedHeaderHashHex", toDelete).findAll().let { blocksToDelete ->
                blocksToDelete.forEach { block ->
                    block.transactions?.let { transactions ->
                        transactions.forEach { transaction ->
                            transaction.inputs.deleteAllFromRealm()
                            transaction.outputs.deleteAllFromRealm()
                        }
                        transactions.deleteAllFromRealm()
                    }
                }
                blocksToDelete.deleteAllFromRealm()
            }
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
