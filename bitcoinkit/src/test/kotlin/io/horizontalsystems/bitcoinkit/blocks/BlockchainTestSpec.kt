package io.horizontalsystems.bitcoinkit.blocks

import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.bitcoinkit.storage.BlockHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.mockito.Mockito.mock
import org.mockito.invocation.InvocationOnMock
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class BlockchainTestSpec : Spek({
    lateinit var blockchain: Blockchain
    lateinit var mockedBlocks: MockedBlocks

    val storage = mock(IStorage::class.java)
    val network = mock(Network::class.java)
    val dataListener = mock(IBlockchainDataListener::class.java)

    val prevHash = byteArrayOf(1)
    val merkleBlock = mock(MerkleBlock::class.java)
    val blockHeader = mock(BlockHeader::class.java)
    val block = mock(Block::class.java)

    val callbackInvoke: (InvocationOnMock) -> Unit = {
        (it.arguments[0] as () -> Unit).invoke()
    }

    beforeEachTest {
        whenever(storage.inTransaction(any())).then(callbackInvoke)
        whenever(blockHeader.previousBlockHeaderHash).thenReturn(prevHash)
        whenever(blockHeader.merkleRoot).thenReturn(byteArrayOf())
        whenever(merkleBlock.header).thenReturn(blockHeader)

        blockchain = Blockchain(storage, network, dataListener)
    }

    afterEachTest {
        reset(storage, network, dataListener)
        reset(merkleBlock, blockHeader, block)
    }

    describe("#connect") {
        beforeEach {
            whenever(merkleBlock.reversedHeaderHashHex).thenReturn("abc")
            whenever(merkleBlock.header).thenReturn(blockHeader)
        }

        context("when block exists") {
            beforeEach {
                whenever(storage.getBlock(merkleBlock.reversedHeaderHashHex)).thenReturn(block)
            }

            it("returns existing block") {
                assertEquals(block, blockchain.connect(merkleBlock))
            }

            it("doesn't add a block to storage") {
                blockchain.connect(merkleBlock)

                verify(storage, never()).addBlock(block)
                verify(dataListener, never()).onBlockInsert(block)
            }
        }

        context("when block doesn't exist") {
            beforeEach {
                whenever(storage.getBlock(merkleBlock.reversedHeaderHashHex)).thenReturn(null)
            }

            context("when block is not in chain") {
                beforeEach {
                    whenever(storage.getBlock(merkleBlock.header.previousBlockHeaderHash.reversedArray().toHexString())).thenReturn(null)
                }

                it("throws BlockValidatorError.noPreviousBlock error") {
                    try {
                        blockchain.connect(merkleBlock)
                    } catch (e: Exception) {
                        if (e !is BlockValidatorException.NoPreviousBlock) {
                            fail("Expected No PreviousBlock exception to be thrown")
                        }
                    }
                }
            }

            context("when block is in chain") {
                beforeEach {
                    whenever(storage.getBlock(merkleBlock.header.previousBlockHeaderHash.reversedArray().toHexString())).thenReturn(block)
                }

                context("when block is invalid") {
                    it("doesn't add a block to storage") {
                        whenever(network.validateBlock(any(), any())).thenThrow(BlockValidatorException.WrongPreviousHeader())

                        try {
                            blockchain.connect(merkleBlock)
                        } catch (e: Exception) {
                        }

                        verify(storage, never()).addBlock(any())
                    }
                }

                context("when block is valid") {

                    it("adds block to database") {
                        try {
                            blockchain.connect(merkleBlock)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        verify(storage).addBlock(any())
                        verify(network).validateBlock(any(), any())
                        verify(dataListener).onBlockInsert(any())
                    }
                }
            }
        }
    }

    describe("#forceAdd") {
        lateinit var connectedBlock: Block

        val height = 1

        beforeEach {
            // whenever(storage.getBlock(merkleBlock.reversedHeaderHashHex)).thenReturn(null)
            // whenever(storage.addBlock(any()))

            connectedBlock = blockchain.forceAdd(merkleBlock, height)
        }

        it("doesn't validate block") {
            verify(network, never()).validateBlock(any(), any())
        }

        it("adds block to database") {
            verify(storage).addBlock(any())
            verify(dataListener).onBlockInsert(connectedBlock)
        }
    }

    describe("#handleFork") {

        context("when no fork found") {
            val blocksInChain = sortedMapOf(1 to "InChain1", 2 to "InChain2", 3 to "InChain3")
            val newBlocks = sortedMapOf(4 to "NewBlock4", 5 to "NewBlock5", 6 to "NewBlock6")

            beforeEach {
                mockedBlocks = MockedBlocks(storage, blockHeader).create(blocksInChain, newBlocks)
            }

            it("makes new blocks not stale") {
                blockchain.handleFork()

                argumentCaptor<Block>().apply {
                    verify(storage, times(3)).updateBlock(capture())

                    mockedBlocks.newBlocks.forEachIndexed { index, block ->
                        assertEquals(false, allValues[index].stale)
                        assertEquals(block.headerHash, allValues[index].headerHash)
                    }
                }
            }
        }

        context("when fork found and new blocks leaf is longer") {
            val blocksInChain = sortedMapOf(1 to "InChain1", 2 to "InChain2", 3 to "InChain3")
            val newBlocks = sortedMapOf(2 to "NewBlock2", 3 to "NewBlock3", 4 to "NewBlock4")

            beforeEach {
                mockedBlocks = MockedBlocks(storage, blockHeader).create(blocksInChain, newBlocks)
            }

            it("deletes old blocks in chain after the fork") {
                blockchain.handleFork()

                verify(storage).deleteBlocks(mockedBlocks.blocksInChain.takeLast(2))
                verify(storage, never()).deleteBlocks(mockedBlocks.newBlocks)
                verify(dataListener).onTransactionsDelete(mockedBlocks.blocksInChainTransactionHexes.takeLast(2))
            }

            it("makes new blocks not stale") {
                blockchain.handleFork()

                argumentCaptor<Block>().apply {
                    verify(storage, times(3)).updateBlock(capture())

                    mockedBlocks.newBlocks.forEachIndexed { index, block ->
                        assertEquals(false, allValues[index].stale)
                        assertEquals(block.headerHash, allValues[index].headerHash)
                    }
                }
            }
        }

        context("when fork found and new blocks leaf is shorter") {
            val blocksInChain = sortedMapOf(1 to "InChain1", 2 to "InChain2", 3 to "InChain3", 4 to "InChain4")
            val newBlocks = sortedMapOf(2 to "NewBlock2", 3 to "NewBlock3")

            beforeEach {
                mockedBlocks = MockedBlocks(storage, blockHeader).create(blocksInChain, newBlocks)
            }

            it("deletes new blocks") {
                blockchain.handleFork()

                verify(storage).deleteBlocks(mockedBlocks.newBlocks)
                verify(storage, never()).deleteBlocks(mockedBlocks.blocksInChain.takeLast(2))
                verify(dataListener).onTransactionsDelete(mockedBlocks.newBlocksTransactionHexes)
            }
        }

        context("when fork exists and two leafs are equal") {
            val blocksInChain = sortedMapOf(1 to "InChain1", 2 to "InChain2", 3 to "InChain3")
            val newBlocks = sortedMapOf(2 to "NewBlock2", 3 to "NewBlock3")

            beforeEach {
                mockedBlocks = MockedBlocks(storage, blockHeader).create(blocksInChain, newBlocks)
            }

            it("deletes new blocks") {
                blockchain.handleFork()

                verify(storage).deleteBlocks(mockedBlocks.newBlocks)
                verify(storage, never()).deleteBlocks(mockedBlocks.blocksInChain.takeLast(2))
                verify(dataListener).onTransactionsDelete(mockedBlocks.newBlocksTransactionHexes)
            }
        }

        context("when no new(stale) blocks found") {
            val blocksInChain = sortedMapOf(1 to "InChain1", 2 to "InChain2", 3 to "InChain3")
            val newBlocks = sortedMapOf<Int, String>()

            beforeEach {
                MockedBlocks(storage, blockHeader).create(blocksInChain, newBlocks)
            }

            it("does not do nothing") {
                blockchain.handleFork()

                verify(storage, never()).deleteBlocks(any())
                verify(storage, never()).updateBlock(any())
                verify(dataListener, never()).onTransactionsDelete(any())
            }
        }

        context("when no blocks in chain") {
            val blocksInChain = sortedMapOf<Int, String>()
            val newBlocks = sortedMapOf(2 to "NewBlock2", 3 to "NewBlock3", 4 to "NewBlock4")

            beforeEach {
                mockedBlocks = MockedBlocks(storage, blockHeader).create(blocksInChain, newBlocks)
            }

            it("makes new blocks not stale") {
                blockchain.handleFork()

                verify(storage, never()).deleteBlocks(any())

                argumentCaptor<Block>().apply {
                    verify(storage, times(3)).updateBlock(capture())

                    mockedBlocks.newBlocks.forEachIndexed { index, block ->
                        assertEquals(false, allValues[index].stale)
                        assertEquals(block.headerHash, allValues[index].headerHash)
                    }
                }
            }
        }

    }
})

class MockedBlocks(private val storage: IStorage, private val blockHeader: BlockHeader) {
    var newBlocks = mutableListOf<Block>()
    var blocksInChain = mutableListOf<Block>()
    var newBlocksTransactionHexes = mutableListOf<String>()
    var blocksInChainTransactionHexes = mutableListOf<String>()

    fun create(_blocksInChain: Map<Int, String>, _newBlocks: Map<Int, String>): MockedBlocks {
        _blocksInChain.forEach { height, id ->
            val block = Block(blockHeader, height)
            block.stale = false
            val transaction = mockTransaction(block)

            whenever(storage.getBlockTransactions(block)).thenReturn(listOf(transaction))

            blocksInChain.add(block)
            blocksInChainTransactionHexes.add(transaction.hashHexReversed)
        }

        _newBlocks.forEach { height, id ->
            val block = Block(blockHeader, height)
            block.stale = false

            val transaction = mockTransaction(block)
            whenever(storage.getBlockTransactions(block)).thenReturn(listOf(transaction))

            newBlocks.add(block)
            newBlocksTransactionHexes.add(transaction.hashHexReversed)
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
        val transaction = mock(Transaction::class.java)

        // whenever(transaction.block).thenReturn(block)
        whenever(transaction.hash).thenReturn(block.headerHash)
        whenever(transaction.hashHexReversed).thenReturn(block.headerHashReversedHex)

        return transaction
    }
}
