package io.horizontalsystems.bitcoinkit.blocks

import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.network.Network
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort

class Blockchain(private val network: Network, private val dataListener: IBlockchainDataListener) {

    fun connect(merkleBlock: MerkleBlock, realm: Realm): Block {
        val blockInDB = realm.where(Block::class.java)
                .equalTo("headerHash", merkleBlock.blockHash)
                .findFirst()

        if (blockInDB != null) {
            return blockInDB
        }

        val parentBlock = realm.where(Block::class.java)
                .equalTo("headerHash", merkleBlock.header.prevHash)
                .findFirst() ?: throw BlockValidatorException.NoPreviousBlock()

        val block = Block(merkleBlock.header, parentBlock)
        network.validateBlock(block, parentBlock)

        block.stale = true

        return addBlockAndNotify(block, realm)
    }

    fun forceAdd(merkleBlock: MerkleBlock, height: Int, realm: Realm): Block {
        return addBlockAndNotify(Block(merkleBlock.header, height), realm)
    }

    fun handleFork(realm: Realm) {
        val firstStaleHeight = realm.where(Block::class.java)
                .equalTo("stale", true)
                .sort("height")
                .findFirst()?.height ?: return

        val lastNotStaleHeight = realm.where(Block::class.java)
                .equalTo("stale", false)
                .sort("height", Sort.DESCENDING)
                .findFirst()?.height ?: 0

        realm.executeTransaction {
            if (firstStaleHeight <= lastNotStaleHeight) {
                val lastStaleHeight = realm.where(Block::class.java)
                        .equalTo("stale", true)
                        .sort("height", Sort.DESCENDING)
                        .findFirst()?.height!!

                val blocksToDelete = if (lastStaleHeight > lastNotStaleHeight) {
                    realm.where(Block::class.java)
                            .equalTo("stale", false)
                            .greaterThanOrEqualTo("height", firstStaleHeight)
                            .findAll()
                } else {
                    realm.where(Block::class.java)
                            .equalTo("stale", true)
                            .findAll()
                }

                deleteBlocks(blocksToDelete)
            }

            realm.where(Block::class.java)
                    .equalTo("stale", true)
                    .findAll()
                    .forEach { staleBlock ->
                        staleBlock.stale = false
                    }
        }
    }

    fun deleteBlocks(blocksToDelete: RealmResults<Block>) {
        val deletedTransactionIds = mutableListOf<String>()

        blocksToDelete.forEach { block ->
            block.transactions?.let { transactions ->
                transactions.forEach { transaction ->
                    transaction.inputs.deleteAllFromRealm()
                    transaction.outputs.deleteAllFromRealm()
                }
                deletedTransactionIds.addAll(transactions.map { it.hashHexReversed })

                transactions.deleteAllFromRealm()
            }
        }
        blocksToDelete.deleteAllFromRealm()

        dataListener.onTransactionsDelete(deletedTransactionIds)
    }

    private fun addBlockAndNotify(block: Block, realm: Realm): Block {
        val managedBlock = realm.copyToRealm(block)

        dataListener.onBlockInsert(managedBlock)
        return managedBlock
    }

}
