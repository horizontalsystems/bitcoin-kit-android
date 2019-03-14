package io.horizontalsystems.bitcoinkit.blocks

import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.network.Network
import io.realm.Realm

class Blockchain(private val storage: IStorage, private val network: Network, private val dataListener: IBlockchainDataListener) {

    fun connect(merkleBlock: MerkleBlock, realm: Realm): Block {
        val blockInDB = storage.getBlock(merkleBlock.reversedHeaderHashHex)
        if (blockInDB != null) {
            return blockInDB
        }

        val parentBlock = storage.getBlock(merkleBlock.header.prevHash.reversedArray().toHexString())
                ?: throw BlockValidatorException.NoPreviousBlock()

        val block = Block(merkleBlock.header, parentBlock)
        network.validateBlock(block, parentBlock)

        block.stale = true

        return addBlockAndNotify(block, realm)
    }

    fun forceAdd(merkleBlock: MerkleBlock, height: Int, realm: Realm): Block {
        return addBlockAndNotify(Block(merkleBlock.header, height), realm)
    }

    fun handleFork() {
        val firstStaleHeight = storage.getBlock(stale = true, sortedHeight = "ASC")
                ?.height ?: return

        val lastNotStaleHeight = storage.getBlock(stale = false, sortedHeight = "DESC")
                ?.height ?: 0

        storage.inTransaction { realm ->
            if (firstStaleHeight <= lastNotStaleHeight) {
                val lastStaleHeight = storage.getBlock(stale = true, sortedHeight = "DESC", realm = realm)
                        ?.height ?: firstStaleHeight

                if (lastStaleHeight > lastNotStaleHeight) {
                    val notStaleBlocks = storage.getBlocks(heightGreaterOrEqualTo = firstStaleHeight, stale = false, realm = realm)
                    deleteBlocks(notStaleBlocks, realm)
                    unstaleAllBlocks(realm)
                } else {
                    val staleBlocks = storage.getBlocks(stale = true, realm = realm)
                    deleteBlocks(staleBlocks, realm)
                }
            } else {
                unstaleAllBlocks(realm)
            }
        }
    }

    fun deleteBlocks(blocksToDelete: List<Block>, realm: Realm) {
        val deletedTransactionIds = mutableListOf<String>()

        blocksToDelete.forEach { block ->
            deletedTransactionIds.addAll(storage.getBlockTransactions(block, realm).map { it.hashHexReversed })
        }

        storage.deleteBlocks(blocksToDelete, realm)

        dataListener.onTransactionsDelete(deletedTransactionIds)
    }

    private fun addBlockAndNotify(block: Block, realm: Realm): Block {
        val managedBlock = storage.copyToRealm(block, realm)

        dataListener.onBlockInsert(managedBlock)
        return managedBlock
    }

    private fun unstaleAllBlocks(realm: Realm) {
        storage.getBlocks(stale = true, realm = realm).forEach { staleBlock ->
            staleBlock.stale = false
            storage.updateBlock(staleBlock, realm)
        }
    }
}
