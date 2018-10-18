package io.horizontalsystems.bitcoinkit.blocks

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.managers.AddressManager
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.network.NetworkParameters
import io.horizontalsystems.bitcoinkit.scripts.ScriptType
import io.horizontalsystems.bitcoinkit.transactions.TransactionProcessor
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

    @Throws(Error.NextBlockNotFull::class)
    fun handleMerkleBlock(merkleBlock: MerkleBlock, fullBlock: Boolean) {
        val realm = realmFactory.realm

        val block = blockchainBuilder.connect(merkleBlock, realm)

        var newUnspentOutput = false

        realm.executeTransaction {
            val managedBlock = realm.copyToRealm(block)

            merkleBlock.associatedTransactions.forEach { transaction ->
                transactionProcessor.process(transaction, realm)
                if (transaction.isMine) {
                    val transactionInDB = realm.where(Transaction::class.java).equalTo("hashHexReversed", transaction.hashHexReversed).findFirst()

                    if (transactionInDB != null) {
                        transactionInDB.status = Transaction.Status.RELAYED
                        transactionInDB.block = managedBlock
                    } else {
                        transaction.block = managedBlock
                        realm.insert(transaction)
                    }

                    if (fullBlock && !newUnspentOutput) {
                        for (output in transaction.outputs) {
                            if (output.scriptType == ScriptType.P2WPKH) {
                                newUnspentOutput = true
                            }
                        }
                    }
                }
            }

            if (fullBlock) {
                realm.where(BlockHash::class.java)
                        .equalTo("reversedHeaderHashHex", block.reversedHeaderHashHex)
                        .findFirst()
                        ?.deleteFromRealm()
            }
        }

        if (fullBlock && (addressManager.gapShifts(realm) || newUnspentOutput)) {
            throw Error.NextBlockNotFull
        }

        realm.close()
    }

    fun clearNotFullBlocks() {
        addressManager.fillGap()

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

    object Error {
        object NextBlockNotFull : Exception()
    }

}
