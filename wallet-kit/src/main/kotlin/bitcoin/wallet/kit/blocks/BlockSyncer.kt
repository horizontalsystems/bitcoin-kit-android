package bitcoin.wallet.kit.blocks

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.managers.AddressManager
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.models.BlockHash
import bitcoin.wallet.kit.models.MerkleBlock
import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.wallet.kit.transactions.TransactionProcessor
import io.realm.Sort

class BlockSyncer(private val realmFactory: RealmFactory,
                  private val blockchainBuilder: BlockchainBuilder,
                  private val transactionProcessor: TransactionProcessor,
                  private val addressManager: AddressManager,
                  private val network: NetworkParameters) {

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
    fun clearBlockHashes() {
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

    @Throws
    fun handleMerkleBlocks(merkleBlocks: List<MerkleBlock>) {
        if (merkleBlocks.isEmpty()) return

        val realm = realmFactory.realm

        val blocks = blockchainBuilder.buildChain(merkleBlocks, realm)

        val gapData = transactionProcessor.getGapData(merkleBlocks, realm)

        realm.executeTransaction {
            var lastSavedBlock: Block? = null

            for (merkleBlock in merkleBlocks) {
                val block = blocks[merkleBlock.reversedHeaderHashHex] ?: continue

                lastSavedBlock?.let {
                    block.previousBlock = it
                }

                val blockManaged = realm.copyToRealm(block)

                merkleBlock.associatedTransactions.forEach { transaction ->
                    transactionProcessor.link(transaction, realm)
                    if (transaction.isMine) {
                        transaction.block = blockManaged
                        realm.insert(transaction)
                    }
                }

                lastSavedBlock = blockManaged

                if (merkleBlock == gapData.firstGapShiftMerkleBlock) {
                    break
                }
            }

            lastSavedBlock?.let {
                realm.where(BlockHash::class.java)
                        .equalTo("reversedHeaderHashHex", lastSavedBlock.reversedHeaderHashHex)
                        .findFirst()
                        ?.let {
                            realm.where(BlockHash::class.java)
                                    .lessThanOrEqualTo("order", it.order)
                                    .findAll()
                                    .deleteAllFromRealm()
                        }
            }
        }

        addressManager.fillGap(gapData.lastUsedExternalKey, gapData.lastUsedInternalKey)

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
