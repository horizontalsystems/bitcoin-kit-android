package io.horizontalsystems.bitcoinkit.blocks

import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.Header
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.network.Network
import io.realm.Realm
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.mockito.Mockito.anyObject
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

    val merkleBlock = mock(MerkleBlock::class.java)
    val header = mock(Header::class.java)
    val block = mock(Block::class.java)
    val realm = mock(Realm::class.java)
    val answerRealmInstance: (InvocationOnMock) -> Unit = {
        (it.arguments[0] as (Realm) -> Unit).invoke(realm)
    }

    beforeEachTest {
        whenever(storage.realmInstance(any())).then(answerRealmInstance)
        whenever(storage.inTransaction(any())).then(answerRealmInstance)

        blockchain = Blockchain(storage, network, dataListener)
    }

    afterEachTest {
        reset(storage, network, dataListener)
        reset(merkleBlock, header, block)
    }

    describe("#connect") {
        beforeEach {
            whenever(merkleBlock.reversedHeaderHashHex).thenReturn("abc")
        }

        context("when block exists") {
            beforeEach {
                whenever(storage.getBlock(merkleBlock.reversedHeaderHashHex)).thenReturn(block)
            }

            it("returns existing block") {
                assertEquals(block, blockchain.connect(merkleBlock, realm))
            }

            it("doesn't add a block to storage") {
                blockchain.connect(merkleBlock, realm)

                verify(storage, never()).copyToRealm(block, realm)
                verify(dataListener, never()).onBlockInsert(block)
            }
        }

        context("when block doesn't exist") {
            val prevHash = byteArrayOf(1)

            beforeEach {
                whenever(header.prevHash).thenReturn(prevHash)
                whenever(header.hash).thenReturn(prevHash)
                whenever(merkleBlock.header).thenReturn(header)
                whenever(storage.getBlock(merkleBlock.reversedHeaderHashHex)).thenReturn(null)
            }

            context("when block is not in chain") {
                beforeEach {
                    whenever(storage.getBlock(merkleBlock.header.prevHash.reversedArray().toHexString())).thenReturn(null)
                }

                it("throws BlockValidatorError.noPreviousBlock error") {
                    try {
                        blockchain.connect(merkleBlock, realm)
                    } catch (e: Exception) {
                        if (e !is BlockValidatorException.NoPreviousBlock) {
                            fail("Expected No PreviousBlock exception to be thrown")
                        }
                    }
                }
            }

            context("when block is in chain") {
                beforeEach {
                    whenever(storage.getBlock(merkleBlock.header.prevHash.reversedArray().toHexString())).thenReturn(block)
                }

                context("when block is invalid") {
                    it("doesn't add a block to storage") {
                        whenever(network.validateBlock(any(), any())).thenThrow(BlockValidatorException.WrongPreviousHeader())

                        try {
                            blockchain.connect(merkleBlock, realm)
                        } catch (e: Exception) {
                        }

                        verify(storage, never()).copyToRealm<Block>(any(), any())
                    }
                }

                context("when block is valid") {
                    beforeEach {
                        whenever(storage.copyToRealm<Block>(any(), any())).thenReturn(block)
                    }

                    it("adds block to database") {
                        try {
                            blockchain.connect(merkleBlock, realm)
                        } catch (e: Exception) {
                        }

                        verify(storage).copyToRealm(anyObject(), any())
                        verify(network).validateBlock(any(), any())
                        verify(dataListener).onBlockInsert(block)
                    }
                }
            }
        }
    }

    describe("#forceAdd") {
        lateinit var connectedBlock: Block

        val height = 1
        val prevHash = byteArrayOf(1)

        beforeEach {
            whenever(header.prevHash).thenReturn(prevHash)
            whenever(header.hash).thenReturn(prevHash)
            whenever(merkleBlock.header).thenReturn(header)
            whenever(storage.getBlock(merkleBlock.reversedHeaderHashHex)).thenReturn(null)
            whenever(storage.copyToRealm<Block>(any(), any())).thenReturn(block)

            connectedBlock = blockchain.forceAdd(merkleBlock, height, realm)
        }

        it("doesn't validate block") {
            verify(network, never()).validateBlock(any(), any())
        }

        it("adds block to database") {
            verify(storage).copyToRealm(anyObject(), any())
            verify(dataListener).onBlockInsert(connectedBlock)
        }
    }

    describe("#handleFork") {

        context("when no fork found") {
            val blocksInChain = sortedMapOf(1 to "InChain1", 2 to "InChain2", 3 to "InChain3")
            val newBlocks = sortedMapOf(4 to "NewBlock4", 5 to "NewBlock5", 6 to "NewBlock6")

            beforeEach {
                mockedBlocks = MockedBlocks(storage, realm).create(blocksInChain, newBlocks)
            }

            it("makes new blocks not stale") {
                blockchain.handleFork()

                argumentCaptor<Block>().apply {
                    verify(storage, times(3)).updateBlock(capture(), any())

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
                mockedBlocks = MockedBlocks(storage, realm).create(blocksInChain, newBlocks)
            }

            it("deletes old blocks in chain after the fork") {
                blockchain.handleFork()

                verify(storage).deleteBlocks(mockedBlocks.blocksInChain.takeLast(2), realm)
                verify(storage, never()).deleteBlocks(mockedBlocks.newBlocks, realm)
                verify(dataListener).onTransactionsDelete(mockedBlocks.blocksInChainTransactionHexes.takeLast(2))
            }

            it("makes new blocks not stale") {
                blockchain.handleFork()

                argumentCaptor<Block>().apply {
                    verify(storage, times(3)).updateBlock(capture(), any())

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
                mockedBlocks = MockedBlocks(storage, realm).create(blocksInChain, newBlocks)
            }

            it("deletes new blocks") {
                blockchain.handleFork()

                verify(storage).deleteBlocks(mockedBlocks.newBlocks, realm)
                verify(storage, never()).deleteBlocks(mockedBlocks.blocksInChain.takeLast(2), realm)
                verify(dataListener).onTransactionsDelete(mockedBlocks.newBlocksTransactionHexes)
            }
        }

        context("when fork exists and two leafs are equal") {
            val blocksInChain = sortedMapOf(1 to "InChain1", 2 to "InChain2", 3 to "InChain3")
            val newBlocks = sortedMapOf(2 to "NewBlock2", 3 to "NewBlock3")

            beforeEach {
                mockedBlocks = MockedBlocks(storage, realm).create(blocksInChain, newBlocks)
            }

            it("deletes new blocks") {
                blockchain.handleFork()

                verify(storage).deleteBlocks(mockedBlocks.newBlocks, realm)
                verify(storage, never()).deleteBlocks(mockedBlocks.blocksInChain.takeLast(2), realm)
                verify(dataListener).onTransactionsDelete(mockedBlocks.newBlocksTransactionHexes)
            }
        }

        context("when no new(stale) blocks found") {
            val blocksInChain = sortedMapOf(1 to "InChain1", 2 to "InChain2", 3 to "InChain3")
            val newBlocks = sortedMapOf<Int, String>()

            beforeEach {
                MockedBlocks(storage, realm).create(blocksInChain, newBlocks)
            }

            it("does not do nothing") {
                blockchain.handleFork()

                verify(storage, never()).deleteBlocks(any(), any())
                verify(storage, never()).updateBlock(any(), any())
                verify(dataListener, never()).onTransactionsDelete(any())
            }
        }

        context("when no blocks in chain") {
            val blocksInChain = sortedMapOf<Int, String>()
            val newBlocks = sortedMapOf(2 to "NewBlock2", 3 to "NewBlock3", 4 to "NewBlock4")

            beforeEach {
                mockedBlocks = MockedBlocks(storage, realm).create(blocksInChain, newBlocks)
            }

            it("makes new blocks not stale") {
                blockchain.handleFork()

                verify(storage, never()).deleteBlocks(any(), any())

                argumentCaptor<Block>().apply {
                    verify(storage, times(3)).updateBlock(capture(), any())

                    mockedBlocks.newBlocks.forEachIndexed { index, block ->
                        assertEquals(false, allValues[index].stale)
                        assertEquals(block.headerHash, allValues[index].headerHash)
                    }
                }
            }
        }

    }
})

class MockedBlocks(private val storage: IStorage, private val realm: Realm) {
    var newBlocks = mutableListOf<Block>()
    var blocksInChain = mutableListOf<Block>()
    var newBlocksTransactionHexes = mutableListOf<String>()
    var blocksInChainTransactionHexes = mutableListOf<String>()

    fun create(_blocksInChain: Map<Int, String>, _newBlocks: Map<Int, String>): MockedBlocks {
        _blocksInChain.forEach { height, id ->
            val block = Block(id.toByteArray(), height)
            block.stale = false
            val transaction = mockTransaction(block)

            whenever(storage.getBlockTransactions(block, realm)).thenReturn(listOf(transaction))

            blocksInChain.add(block)
            blocksInChainTransactionHexes.add(transaction.hashHexReversed)
        }

        _newBlocks.forEach { height, id ->
            val block = Block(id.toByteArray(), height)
            block.stale = false

            val transaction = mockTransaction(block)
            whenever(storage.getBlockTransactions(block, realm)).thenReturn(listOf(transaction))

            newBlocks.add(block)
            newBlocksTransactionHexes.add(transaction.hashHexReversed)
        }

        whenever(storage.getBlocks(stale = true, realm = realm)).thenReturn(newBlocks)

        newBlocks.firstOrNull()?.let { firstStale ->
            whenever(storage.getBlock(stale = true, sortedHeight = "ASC"))
                    .thenReturn(firstStale)

            newBlocks.lastOrNull()?.let { lastStale ->
                whenever(storage.getBlock(stale = true, sortedHeight = "DESC")).thenReturn(lastStale)
                whenever(storage.getBlock(stale = true, sortedHeight = "DESC", realm = realm)).thenReturn(lastStale)

                val inChainBlocksAfterForkPoint = blocksInChain.filter { it.height >= firstStale.height }
                whenever(storage.getBlocks(heightGreaterOrEqualTo = firstStale.height, stale = false, realm = realm))
                        .thenReturn(inChainBlocksAfterForkPoint)
            }
        }

        blocksInChain.lastOrNull()?.let {
            whenever(storage.getBlock(stale = eq(false), sortedHeight = eq("DESC"), realm = anyObject())).thenReturn(it)
        }

        return this
    }

    private fun mockTransaction(block: Block): Transaction {
        val transaction = mock(Transaction::class.java)

        whenever(transaction.block).thenReturn(block)
        whenever(transaction.hash).thenReturn(block.headerHash)
        whenever(transaction.hashHexReversed).thenReturn(block.reversedHeaderHashHex)

        return transaction
    }
}
