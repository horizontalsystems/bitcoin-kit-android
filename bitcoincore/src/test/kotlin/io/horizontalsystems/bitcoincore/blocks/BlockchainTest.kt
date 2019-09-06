package io.horizontalsystems.bitcoincore.blocks

import com.nhaarman.mockitokotlin2.*
import io.horizontalsystems.bitcoincore.MockedBlocks
import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoincore.blocks.validators.IBlockValidator
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.models.MerkleBlock
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.mockito.Mockito.mock
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object BlockchainTest : Spek({

    lateinit var blockchain: Blockchain
    lateinit var mockedBlocks: MockedBlocks

    val storage = mock(IStorage::class.java)
    val blockValidator = mock(IBlockValidator::class.java)
    val dataListener = mock(IBlockchainDataListener::class.java)

    val prevHash = byteArrayOf(1)
    val merkleBlock = mock(MerkleBlock::class.java)
    val blockHeader = mock(BlockHeader::class.java)
    val block = mock(Block::class.java)

    beforeEachTest {
        whenever(blockHeader.previousBlockHeaderHash).thenReturn(prevHash)
        whenever(blockHeader.merkleRoot).thenReturn(byteArrayOf())
        whenever(blockHeader.hash).thenReturn(byteArrayOf(1, 2))
        whenever(merkleBlock.header).thenReturn(blockHeader)
        whenever(merkleBlock.blockHash).thenReturn(byteArrayOf(1, 3))

        blockchain = Blockchain(storage, blockValidator, dataListener)
    }

    afterEachTest {
        reset(storage, blockValidator, dataListener)
        reset(merkleBlock, blockHeader, block)
    }

    describe("#connect") {
        beforeEach {
            whenever(merkleBlock.blockHash).thenReturn(byteArrayOf(1, 2, 3))
            whenever(merkleBlock.header).thenReturn(blockHeader)
        }

        context("when block exists") {
            beforeEach {
                whenever(storage.getBlock(merkleBlock.blockHash)).thenReturn(block)
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
                whenever(storage.getBlock(merkleBlock.blockHash)).thenReturn(null)
            }

            context("when block is not in chain") {
                beforeEach {
                    whenever(storage.getBlock(merkleBlock.header.previousBlockHeaderHash)).thenReturn(null)
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
                    whenever(storage.getBlock(merkleBlock.header.previousBlockHeaderHash)).thenReturn(block)
                }

                context("when block is invalid") {
                    it("doesn't add a block to storage") {
                        whenever(blockValidator.validate(any(), any())).thenThrow(BlockValidatorException.WrongPreviousHeader())

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
                        verify(blockValidator).validate(any(), any())
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
            connectedBlock = blockchain.forceAdd(merkleBlock, height)
        }

        it("doesn't validate block") {
            verify(blockValidator, never()).validate(any(), any())
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

                verify(storage).unstaleAllBlocks()
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
                verify(dataListener).onTransactionsDelete(mockedBlocks.blocksInChainTransactionHashes.takeLast(2))
            }

            it("makes new blocks not stale") {
                blockchain.handleFork()

                verify(storage).unstaleAllBlocks()
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
                verify(dataListener).onTransactionsDelete(mockedBlocks.newBlocksTransactionHashes)
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
                verify(dataListener).onTransactionsDelete(mockedBlocks.newBlocksTransactionHashes)
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
                verify(storage).unstaleAllBlocks()
            }
        }

    }
})
