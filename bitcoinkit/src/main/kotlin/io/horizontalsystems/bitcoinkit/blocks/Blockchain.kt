package io.horizontalsystems.bitcoinkit.blocks

import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.extensions.toReversedHex
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.network.Network

class Blockchain(private val storage: IStorage, private val network: Network, private val dataListener: IBlockchainDataListener) {

    fun connect(merkleBlock: MerkleBlock): Block {
        val blockInDB = storage.getBlock(merkleBlock.reversedHeaderHashHex)
        if (blockInDB != null) {
            return blockInDB
        }

        val parentBlock = storage.getBlock(merkleBlock.header.previousBlockHeaderHash.toReversedHex())
                ?: throw BlockValidatorException.NoPreviousBlock()

        val block = Block(merkleBlock.header, parentBlock)
        network.validateBlock(block, parentBlock)

        block.stale = true

        return addBlockAndNotify(block)
    }

    fun forceAdd(merkleBlock: MerkleBlock, height: Int): Block {
        return addBlockAndNotify(Block(merkleBlock.header, height))
    }

    fun handleFork() {
        val firstStaleHeight = storage.getBlock(stale = true, sortedHeight = "ASC")
                ?.height ?: return

        val lastNotStaleHeight = storage.getBlock(stale = false, sortedHeight = "DESC")
                ?.height ?: 0

        storage.inTransaction {
            if (firstStaleHeight <= lastNotStaleHeight) {
                val lastStaleHeight = storage.getBlock(stale = true, sortedHeight = "DESC")?.height ?: firstStaleHeight

                if (lastStaleHeight > lastNotStaleHeight) {
                    val notStaleBlocks = storage.getBlocks(heightGreaterOrEqualTo = firstStaleHeight, stale = false)
                    deleteBlocks(notStaleBlocks)
                    unstaleAllBlocks()
                } else {
                    val staleBlocks = storage.getBlocks(stale = true)
                    deleteBlocks(staleBlocks)
                }
            } else {
                unstaleAllBlocks()
            }
        }
    }

    fun deleteBlocks(blocksToDelete: List<Block>) {
        val deletedTransactionIds = mutableListOf<String>()

        blocksToDelete.forEach { block ->
            deletedTransactionIds.addAll(storage.getBlockTransactions(block).map { it.hashHexReversed })
        }

        storage.deleteBlocks(blocksToDelete)

        dataListener.onTransactionsDelete(deletedTransactionIds)
    }

    private fun addBlockAndNotify(block: Block): Block {
        storage.addBlock(block)
        dataListener.onBlockInsert(block)

        return block
    }

    private fun unstaleAllBlocks() {
        storage.getBlocks(stale = true).forEach { staleBlock ->
            staleBlock.stale = false
            storage.updateBlock(staleBlock)
        }
    }
}
