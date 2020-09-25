package io.horizontalsystems.bitcoincore.blocks

import com.nhaarman.mockitokotlin2.*
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.managers.BloomFilterManager
import io.horizontalsystems.bitcoincore.managers.PublicKeyManager
import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.models.Checkpoint
import io.horizontalsystems.bitcoincore.models.MerkleBlock
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.transactions.BlockTransactionProcessor
import org.junit.Assert.assertEquals
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object BlockSyncerTest : Spek({

    lateinit var blockSyncer: BlockSyncer

    val storage = mock(IStorage::class.java)
    val blockchain = mock(Blockchain::class.java)
    val transactionProcessor = mock(BlockTransactionProcessor::class.java)
    val publicKeyManager = mock(PublicKeyManager::class.java)
    val network = mock(Network::class.java)

    val state = mock(BlockSyncer.State::class.java)
    val checkpointBlock = mock(Block::class.java)
    val checkpoint = mock(Checkpoint::class.java)

    beforeEachTest {
        whenever(network.lastCheckpoint).thenReturn(checkpoint)
        whenever(checkpoint.block).thenReturn(checkpointBlock)
        whenever(checkpoint.additionalBlocks).thenReturn(listOf())

        whenever(checkpointBlock.height).thenReturn(1)

        whenever(storage.blocksCount()).thenReturn(1)
        whenever(storage.lastBlock()).thenReturn(null)

        blockSyncer = BlockSyncer(storage, blockchain, transactionProcessor, publicKeyManager, network.lastCheckpoint, state)
    }

    afterEachTest {
        reset(storage, blockchain, transactionProcessor, publicKeyManager, network, state)
    }

    describe("#init") {
        beforeEach {
            reset(storage)
        }

        context("when there are some saved blocks") {
            beforeEach {
                whenever(storage.blocksCount()).thenReturn(1)
                whenever(storage.lastBlock()).thenReturn(checkpointBlock)

                BlockSyncer(storage, blockchain, transactionProcessor, publicKeyManager, network.lastCheckpoint, state)
            }

            it("does not saves block to storage") {
                verify(storage, never()).saveBlock(checkpointBlock)
            }
        }
    }

    describe("#localDownloadedBestBlockHeight") {
        context("when there is no block in storage") {
            it("returns 0 as default height") {
                whenever(storage.lastBlock()).thenReturn(null)

                assertEquals(0, blockSyncer.localDownloadedBestBlockHeight)
            }
        }

        context("when there are some blocks in storage") {
            it("returns block height") {
                whenever(storage.lastBlock()).thenReturn(checkpointBlock)

                assertEquals(checkpointBlock.height, blockSyncer.localDownloadedBestBlockHeight)
            }
        }
    }

    describe("#localKnownBestBlockHeight") {
        val blockHash = BlockHash("abc".hexToByteArray(), 1)

        context("when no BlockHashes") {
            beforeEach {
                whenever(storage.getBlockchainBlockHashes()).thenReturn(listOf())
                whenever(storage.blocksCount(listOf())).thenReturn(0)
            }

            context("when no blocks") {
                it("returns 0") {
                    assertEquals(0, blockSyncer.localKnownBestBlockHeight)
                }
            }

            context("with some blocks") {
                it("returns last block height + blocks count") {
                    whenever(storage.lastBlock()).thenReturn(checkpointBlock)

                    assertEquals(checkpointBlock.height, blockSyncer.localKnownBestBlockHeight)
                }
            }
        }

        context("when there are some BlockHashes which haven't downloaded blocks") {
            beforeEach {
                whenever(storage.getBlockchainBlockHashes()).thenReturn(listOf(blockHash))
                whenever(storage.blocksCount(listOf(blockHash.headerHash))).thenReturn(0)
            }

            it("returns lastBLock + BlockHashes count") {
                assertEquals(1, blockSyncer.localKnownBestBlockHeight)

                whenever(storage.lastBlock()).thenReturn(checkpointBlock)
                assertEquals(checkpointBlock.height + 1, blockSyncer.localKnownBestBlockHeight)
            }
        }

        context("when there are some BlockHashes which have downloaded blocks") {
            beforeEach {
                whenever(storage.getBlockchainBlockHashes()).thenReturn(listOf(blockHash))
                whenever(storage.blocksCount(listOf(blockHash.headerHash))).thenReturn(1)
            }

            it("returns lastBLock height") {
                assertEquals(0, blockSyncer.localKnownBestBlockHeight)

                whenever(storage.lastBlock()).thenReturn(checkpointBlock)
                assertEquals(checkpointBlock.height, blockSyncer.localKnownBestBlockHeight)
            }
        }
    }

    describe("#prepareForDownload") {
        val emptyBlocks = mock<List<Block>>()
        val blocksHashes = listOf(byteArrayOf(1))

        beforeEach {
            whenever(storage.getBlocks(any(), any())).thenReturn(emptyBlocks)
            whenever(storage.getBlockHashHeaderHashes(listOf(checkpointBlock.headerHash))).thenReturn(blocksHashes)

            blockSyncer.prepareForDownload()
        }

        it("handles partial blocks") {
            verify(publicKeyManager).fillGap()
            verify(state).iterationHasPartialBlocks = false
        }

        it("clears partial blocks") {
            verify(storage).getBlockHashHeaderHashes(listOf(checkpointBlock.headerHash))
            verify(storage).getBlocks(blocksHashes)
            verify(blockchain).deleteBlocks(any())
        }

        it("clears block hashes") {
            verify(storage).deleteBlockchainBlockHashes()
        }

        it("handles fork") {
            verify(blockchain).handleFork()
        }
    }

    describe("#downloadIterationCompleted") {

        context("when iteration has partial blocks") {

            it("handles partial blocks") {
                whenever(state.iterationHasPartialBlocks).thenReturn(true)
                blockSyncer.downloadIterationCompleted()

                verify(publicKeyManager).fillGap()
                verify(state).iterationHasPartialBlocks = false
            }
        }

        context("when iteration has no partial blocks") {

            it("does not handles partial blocks") {
                whenever(state.iterationHasPartialBlocks).thenReturn(false)
                blockSyncer.downloadIterationCompleted()

                verify(state).iterationHasPartialBlocks

                verifyNoMoreInteractions(state)
                verifyNoMoreInteractions(publicKeyManager)
            }
        }
    }

    describe("#downloadCompleted") {
        it("handles fork") {
            blockSyncer.downloadCompleted()

            verify(blockchain).handleFork()
        }
    }

    describe("#downloadFailed") {
        val emptyBlocks = mock<List<Block>>()
        val blocksHashes = listOf(byteArrayOf(1))

        beforeEach {
            whenever(storage.getBlocks(any(), any())).thenReturn(emptyBlocks)
            whenever(storage.getBlockHashHeaderHashes(listOf(checkpointBlock.headerHash))).thenReturn(blocksHashes)

            blockSyncer.downloadFailed()
        }

        it("handles partial blocks") {
            verify(publicKeyManager).fillGap()
            verify(state).iterationHasPartialBlocks = false
        }

        it("clears partial blocks") {
            verify(storage).getBlockHashHeaderHashes(listOf(checkpointBlock.headerHash))
            verify(storage).getBlocks(blocksHashes)
            verify(blockchain).deleteBlocks(any())
        }

        it("clears block hashes") {
            verify(storage).deleteBlockchainBlockHashes()
        }

        it("handles fork") {
            verify(blockchain).handleFork()
        }
    }

    describe("#getBlockHashes") {
        val listOfBlockHashes = listOf(BlockHash("abc".hexToByteArray(), 1))

        it("returns first 500 block hashes") {
            whenever(storage.getBlockHashesSortedBySequenceAndHeight(limit = 500))
                    .thenReturn(listOfBlockHashes)

            assertEquals(listOfBlockHashes, blockSyncer.getBlockHashes())
        }
    }

    describe("#getBlockLocatorHashes") {
        val peerLastBlockHeight = 99

        beforeEach {
            whenever(storage.getLastBlockchainBlockHash()).thenReturn(null)
            whenever(storage.getBlocks(checkpointBlock.height, "height", 10)).thenReturn(listOf())
            whenever(storage.getBlock(peerLastBlockHeight)).thenReturn(null)
        }

        context("when there's no block or block hashes") {
            it("return checkpoint's header hash") {
                assertEquals(listOf(checkpointBlock.headerHash), blockSyncer.getBlockLocatorHashes(peerLastBlockHeight))
            }
        }

        context("when there's blockchain block hashes") {
            val blockHash = BlockHash("cba".hexToByteArray(), 1)

            beforeEach {
                whenever(storage.getLastBlockchainBlockHash()).thenReturn(blockHash)
            }

            it("return last block hash and checkpoint's header hash") {
                assertEquals(listOf(blockHash.headerHash, checkpointBlock.headerHash), blockSyncer.getBlockLocatorHashes(peerLastBlockHeight))
            }
        }

        context("when there's no blockHashes but there are blocks") {
            val block1 = mock(Block::class.java)
            val block2 = mock(Block::class.java)
            val blocks = listOf(block1, block2)

            beforeEach {
                whenever(storage.getBlocks(heightGreaterThan = checkpointBlock.height, sortedBy = "height", limit = 10)).thenReturn(blocks)
            }

            it("returns blocks") {
                val locatorHashes = blockSyncer.getBlockLocatorHashes(peerLastBlockHeight)

                assertEquals(3, locatorHashes.size)
                assertEquals(block1.headerHash, locatorHashes[0])
                assertEquals(block2.headerHash, locatorHashes[1])
                assertEquals(checkpointBlock.headerHash, locatorHashes[2])
            }
        }

        context("when peers last block already exists in storage") {
            val peerBlock = mock(Block::class.java)

            val headerHash = byteArrayOf(1, 2, 3)
            val blockHash = BlockHash(headerHash, 1)

            beforeEach {
                whenever(peerBlock.headerHash).thenReturn(headerHash)

                whenever(storage.getLastBlockchainBlockHash()).thenReturn(blockHash)
                whenever(storage.getBlock(peerLastBlockHeight)).thenReturn(peerBlock)
            }

            it("returns only one header hash") {
                assertEquals(listOf(blockHash.headerHash), blockSyncer.getBlockLocatorHashes(peerLastBlockHeight))
            }
        }
    }

    describe("#addBlockHashes") {
        val existingHashes = listOf(
                byteArrayOf(1, 2, 3),
                byteArrayOf(4, 5, 6))

        val newBlockHashes = listOf(
                byteArrayOf(4, 5, 6),
                byteArrayOf(5, 6, 7))

        beforeEach {
            whenever(storage.getBlockHashHeaderHashes()).thenReturn(existingHashes)
        }

        context("when there's some block hashes") {
            val blockHash = BlockHash(byteArrayOf(1), 99, sequence = 99)

            beforeEach {
                whenever(storage.getLastBlockHash()).thenReturn(blockHash)
            }

            it("set sequence of given block hashes staring from last block hash sequence") {
                blockSyncer.addBlockHashes(newBlockHashes)

                verify(storage).addBlockHashes(argThat {
                    this.size == 1 &&
                            this[0].sequence == blockHash.sequence + 1 &&
                            this[0].headerHash.contentEquals(newBlockHashes[1])
                })
            }
        }

        context("when there's no block hashes") {
            beforeEach {
                whenever(storage.getLastBlockHash()).thenReturn(null)
            }

            it("set sequence of given block hashes staring from 0") {
                blockSyncer.addBlockHashes(newBlockHashes)

                verify(storage).addBlockHashes(argThat {
                    this.size == 1 &&
                            this[0].sequence == 1 &&
                            this[0].headerHash.contentEquals(newBlockHashes[1])
                })
            }
        }
    }

    describe("#handleMerkleBlock") {
        val maxBlockHeight = 100
        val block = mock(Block::class.java)
        val merkleBlock = mock(MerkleBlock::class.java)
        val merkleHeight = 1

        beforeEach {
            whenever(merkleBlock.height).thenReturn(null)
            whenever(merkleBlock.associatedTransactions).thenReturn(mutableListOf())
            whenever(block.height).thenReturn(merkleHeight)
            whenever(block.headerHash).thenReturn(byteArrayOf(1, 2, 3))
            whenever(state.iterationHasPartialBlocks).thenReturn(true)

            whenever(blockchain.connect(merkleBlock)).thenReturn(block)
            whenever(blockchain.forceAdd(merkleBlock, merkleHeight)).thenReturn(block)
        }

        afterEach {
            reset(merkleBlock)
        }

        it("handles merkle block") {
            blockSyncer.handleMerkleBlock(merkleBlock, maxBlockHeight)

            verify(blockchain).connect(merkleBlock)
            verify(transactionProcessor).processReceived(merkleBlock.associatedTransactions, block, state.iterationHasPartialBlocks)
        }

        context("when merkle block height it not null") {

            beforeEach {
                whenever(merkleBlock.height).thenReturn(merkleHeight)
            }

            it("force adds merkle block") {
                blockSyncer.handleMerkleBlock(merkleBlock, maxBlockHeight)

                verify(blockchain).forceAdd(merkleBlock, merkleHeight)
            }
        }

        context("when bloom filter expired while processing transaction") {
            beforeEach {
                whenever(transactionProcessor.processReceived(merkleBlock.associatedTransactions, block, state.iterationHasPartialBlocks))
                        .thenThrow(BloomFilterManager.BloomFilterExpired)
            }

            it("sets state as it left partially handled blocks") {
                blockSyncer.handleMerkleBlock(merkleBlock, maxBlockHeight)

                verify(state).iterationHasPartialBlocks = true
            }
        }

        context("when iteration not have partial blocks") {
            beforeEach {
                whenever(state.iterationHasPartialBlocks).thenReturn(false)
            }

            it("delete block hash") {
                blockSyncer.handleMerkleBlock(merkleBlock, maxBlockHeight)

                verify(storage).deleteBlockHash(block.headerHash)
            }
        }
    }

    describe("#shouldRequest") {
        val block = mock(Block::class.java)
        val hashHash = byteArrayOf(1)

        it("returns true if block exists") {
            whenever(storage.getBlock(hashHash)).thenReturn(block)

            assertEquals(false, blockSyncer.shouldRequest(hashHash))
        }
    }

    describe("#getCheckpointBlock") {
        val bip44Checkpoint = mock<Checkpoint>()
        val bip44CheckpointBlock = mock<Block>()
        val lastCheckpoint = mock<Checkpoint>()
        val lastCheckpointBlock = mock<Block>()
        val lastBlockInDB = mock<Block>()

        beforeEach {
            whenever(network.bip44Checkpoint).thenReturn(bip44Checkpoint)
            whenever(bip44Checkpoint.block).thenReturn(bip44CheckpointBlock)
            whenever(bip44Checkpoint.additionalBlocks).thenReturn(listOf())

            whenever(network.lastCheckpoint).thenReturn(lastCheckpoint)
            whenever(lastCheckpoint.block).thenReturn(lastCheckpointBlock)
            whenever(lastCheckpoint.additionalBlocks).thenReturn(listOf())

            whenever(storage.lastBlock()).thenReturn(lastBlockInDB)
        }

        context("when sync mode is Full") {
            val syncMode = BitcoinCore.SyncMode.Full()

            it("equals to bip44Checkpoint") {
                val actual = BlockSyncer.resolveCheckpoint(syncMode, network, storage)
                assertEquals(bip44Checkpoint, actual)
            }
        }

        context("when sync mode is Api or New") {
            val syncMode = BitcoinCore.SyncMode.Api()

            context("when last block in DB earlier than checkpoint block") {
                beforeEach {
                    whenever(lastBlockInDB.height).thenReturn(100)
                    whenever(lastCheckpointBlock.height).thenReturn(200)
                }

                it("equals to bip44Checkpoint") {
                    val actual = BlockSyncer.resolveCheckpoint(syncMode, network, storage)
                    assertEquals(bip44Checkpoint, actual)
                }
            }

            context("when last block in DB later than checkpoint block") {
                beforeEach {
                    whenever(storage.lastBlock()).thenReturn(lastBlockInDB)
                    whenever(lastBlockInDB.height).thenReturn(200)
                    whenever(lastCheckpointBlock.height).thenReturn(100)
                }

                it("equals to lastCheckpoint") {
                    val actual = BlockSyncer.resolveCheckpoint(syncMode, network, storage)
                    assertEquals(lastCheckpoint, actual)
                }
            }
        }

        context("when DB has no block") {
            beforeEach {
                whenever(storage.lastBlock()).thenReturn(null)
            }

            it("saves checkpoint block to DB") {
                BlockSyncer.resolveCheckpoint(BitcoinCore.SyncMode.Full(), network, storage)

                verify(storage).saveBlock(bip44CheckpointBlock)
            }
        }
    }

})
