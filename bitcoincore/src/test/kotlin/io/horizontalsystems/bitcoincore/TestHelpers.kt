package io.horizontalsystems.bitcoincore

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import org.mockito.Mockito
import java.security.SecureRandom

val random = SecureRandom()

fun randomBytes(size: Int = 32): ByteArray {
    return ByteArray(size).also { random.nextBytes(it) }
}

class MockedBlocks(private val storage: IStorage, private val blockHeader: BlockHeader) {
    var newBlocks = mutableListOf<Block>()
    var blocksInChain = mutableListOf<Block>()
    var newBlocksTransactionHashes = mutableListOf<String>()
    var blocksInChainTransactionHashes = mutableListOf<String>()

    fun create(_blocksInChain: Map<Int, String>, _newBlocks: Map<Int, String>): MockedBlocks {
        _blocksInChain.forEach { height, id ->
            val block = Block(blockHeader, height)
            block.stale = false
            val transaction = mockTransaction(block)

            whenever(storage.getBlockTransactions(block)).thenReturn(listOf(transaction))

            blocksInChain.add(block)
            blocksInChainTransactionHashes.add(transaction.hash.toReversedHex())
        }

        _newBlocks.forEach { height, id ->
            val block = Block(blockHeader, height)
            block.stale = false

            val transaction = mockTransaction(block)
            whenever(storage.getBlockTransactions(block)).thenReturn(listOf(transaction))

            newBlocks.add(block)
            newBlocksTransactionHashes.add(transaction.hash.toReversedHex())
        }

        whenever(storage.getBlocks(stale = true)).thenReturn(newBlocks)

        newBlocks.firstOrNull()?.let { firstStale ->
            whenever(storage.getBlock(stale = true, sortedHeight = "ASC"))
                    .thenReturn(firstStale)

            newBlocks.lastOrNull()?.let { lastStale ->
                whenever(storage.getBlock(stale = true, sortedHeight = "DESC")).thenReturn(lastStale)
                whenever(storage.getBlock(stale = true, sortedHeight = "DESC")).thenReturn(lastStale)

                val inChainBlocksAfterForkPoint = blocksInChain.filter { it.height >= firstStale.height }
                whenever(storage.getBlocks(heightGreaterOrEqualTo = firstStale.height, stale = false))
                        .thenReturn(inChainBlocksAfterForkPoint)
            }
        }

        blocksInChain.lastOrNull()?.let {
            whenever(storage.getBlock(stale = eq(false), sortedHeight = eq("DESC"))).thenReturn(it)
        }

        return this
    }

    private fun mockTransaction(block: Block): Transaction {
        val transaction = Mockito.mock(Transaction::class.java)

        whenever(transaction.hash).thenReturn(block.headerHash)

        return transaction
    }
}
