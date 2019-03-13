package io.horizontalsystems.bitcoinkit.blocks

import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.core.KitStateProvider
import io.horizontalsystems.bitcoinkit.managers.AddressManager
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.bitcoinkit.transactions.TransactionProcessor
import io.realm.Realm
import io.realm.RealmResults
import org.junit.Assert.assertEquals
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.invocation.InvocationOnMock
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class BlockSyncerTest : Spek({

    lateinit var blockSyncer: BlockSyncer

    val storage = mock(IStorage::class.java)
    val blockchain = mock(Blockchain::class.java)
    val transactionProcessor = mock(TransactionProcessor::class.java)
    val addressManager = mock(AddressManager::class.java)
    val bloomFilterManager = mock(BloomFilterManager::class.java)
    val listener = mock(KitStateProvider::class.java)
    val network = mock(Network::class.java)

    val realm = mock(Realm::class.java)
    val state = mock(BlockSyncer.State::class.java)
    val checkpointBlock = mock(Block::class.java)

    val answerRealmInstance: (InvocationOnMock) -> Unit = {
        (it.arguments[0] as (Realm) -> Unit).invoke(realm)
    }

    beforeEachTest {
        whenever(storage.realmInstance(any())).then(answerRealmInstance)
        whenever(storage.inTransaction(any())).then(answerRealmInstance)

        whenever(checkpointBlock.height).thenReturn(1)
        whenever(network.checkpointBlock).thenReturn(checkpointBlock)

        whenever(storage.blocksCount()).thenReturn(1)
        whenever(storage.lastBlock()).thenReturn(null)

        blockSyncer = BlockSyncer(storage, blockchain, transactionProcessor, addressManager, bloomFilterManager, listener, network, state)
    }

    afterEachTest {
        reset(storage, blockchain, transactionProcessor, addressManager, bloomFilterManager, listener, network, state)
        reset(realm)
    }

    describe("#init") {
        beforeEach {
            reset(storage, listener)
        }

        context("when there are some saved blocks") {
            beforeEach {
                whenever(storage.blocksCount()).thenReturn(1)
                whenever(storage.lastBlock()).thenReturn(checkpointBlock)

                BlockSyncer(storage, blockchain, transactionProcessor, addressManager, bloomFilterManager, listener, network, state)
            }

            it("does not saves block to storage") {
                verify(storage, never()).saveBlock(checkpointBlock)
            }

            it("triggers #onInitialBestBlockHeightUpdate event on listener") {
                verify(listener).onInitialBestBlockHeightUpdate(checkpointBlock.height)
            }
        }

        context("when there is no block in storage") {
            beforeEach {
                whenever(storage.blocksCount()).thenReturn(0)
                whenever(storage.lastBlock()).thenReturn(checkpointBlock)

                BlockSyncer(storage, blockchain, transactionProcessor, addressManager, bloomFilterManager, listener, network, state)
            }

            it("saves block to storage") {
                verify(storage).saveBlock(checkpointBlock)
            }

            it("triggers #onInitialBestBlockHeightUpdate event on listener") {
                verify(listener).onInitialBestBlockHeightUpdate(checkpointBlock.height)
                verifyNoMoreInteractions(listener)
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
        val blockHash = BlockHash("abc", 1)

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
                whenever(storage.blocksCount(listOf(blockHash.reversedHeaderHashHex))).thenReturn(0)
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
                whenever(storage.blocksCount(listOf(blockHash.reversedHeaderHashHex))).thenReturn(1)
            }

            it("returns lastBLock height") {
                assertEquals(0, blockSyncer.localKnownBestBlockHeight)

                whenever(storage.lastBlock()).thenReturn(checkpointBlock)
                assertEquals(checkpointBlock.height, blockSyncer.localKnownBestBlockHeight)
            }
        }
    }

    describe("#prepareForDownload") {
        val emptyBlocks = mock<RealmResults<Block>>()

        beforeEach {
            whenever(storage.getBlocks(eq(realm), any())).thenReturn(emptyBlocks)

            blockSyncer.prepareForDownload()
        }

        it("handles partial blocks") {
            verify(addressManager).fillGap()
            verify(bloomFilterManager).regenerateBloomFilter()
            verify(state).iterationHasPartialBlocks = false
        }

        it("clears partial blocks") {
            verify(storage).getBlockHashHeaderHashHexes(checkpointBlock.reversedHeaderHashHex)
            verify(storage).getBlocks(realm, listOf())
            verify(blockchain).deleteBlocks(emptyBlocks)
        }

        it("clears block hashes") {
            verify(storage).deleteBlockchainBlockHashes()
        }

        it("handles fork") {
            verify(blockchain).handleFork(realm)
        }
    }

    describe("#downloadIterationCompleted") {

        context("when iteration has partial blocks") {

            it("handles partial blocks") {
                whenever(state.iterationHasPartialBlocks).thenReturn(true)
                blockSyncer.downloadIterationCompleted()

                verify(addressManager).fillGap()
                verify(bloomFilterManager).regenerateBloomFilter()
                verify(state).iterationHasPartialBlocks = false
            }
        }

        context("when iteration has no partial blocks") {

            it("does not handles partial blocks") {
                whenever(state.iterationHasPartialBlocks).thenReturn(false)
                blockSyncer.downloadIterationCompleted()

                verify(state).iterationHasPartialBlocks

                verifyNoMoreInteractions(state)
                verifyNoMoreInteractions(addressManager)
                verifyNoMoreInteractions(bloomFilterManager)
            }
        }
    }

    describe("#downloadCompleted") {
        it("handles fork") {
            blockSyncer.downloadCompleted()

            verify(blockchain).handleFork(realm)
        }
    }

    describe("#downloadFailed") {
        val emptyBlocks = mock<RealmResults<Block>>()

        beforeEach {
            whenever(storage.getBlocks(eq(realm), any())).thenReturn(emptyBlocks)

            blockSyncer.downloadFailed()
        }

        it("handles partial blocks") {
            verify(addressManager).fillGap()
            verify(bloomFilterManager).regenerateBloomFilter()
            verify(state).iterationHasPartialBlocks = false
        }

        it("clears partial blocks") {
            verify(storage).getBlockHashHeaderHashHexes(checkpointBlock.reversedHeaderHashHex)
            verify(storage).getBlocks(realm, listOf())
            verify(blockchain).deleteBlocks(emptyBlocks)
        }

        it("clears block hashes") {
            verify(storage).deleteBlockchainBlockHashes()
        }

        it("handles fork") {
            verify(blockchain).handleFork(realm)
        }
    }

    describe("#getBlockHashes") {
        val listOfBlockHashes = listOf(BlockHash("abc", 1))

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
            whenever(storage.getBlocks(any(), any(), any())).thenReturn(listOf())
            whenever(storage.getBlock(peerLastBlockHeight)).thenReturn(null)
        }

        context("when there's no block or block hashes") {
            it("return checkpoint's header hash") {
                assertEquals(listOf(checkpointBlock.headerHash), blockSyncer.getBlockLocatorHashes(peerLastBlockHeight))
            }
        }

        context("when there's blockchain block hashes") {
            val blockHash = BlockHash("cba", 1)

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
            whenever(block.reversedHeaderHashHex).thenReturn("abc")
            whenever(state.iterationHasPartialBlocks).thenReturn(true)

            whenever(blockchain.connect(merkleBlock, realm)).thenReturn(block)
            whenever(blockchain.forceAdd(merkleBlock, merkleHeight, realm)).thenReturn(block)
        }

        afterEach {
            reset(merkleBlock)
        }

        it("handles merkle block") {
            blockSyncer.handleMerkleBlock(merkleBlock, maxBlockHeight)

            verify(storage).inTransaction(any())
            verify(blockchain).connect(merkleBlock, realm)
            verify(transactionProcessor).process(merkleBlock.associatedTransactions, block, state.iterationHasPartialBlocks, realm)
            verify(listener).onCurrentBestBlockHeightUpdate(block.height, maxBlockHeight)
        }

        context("when merkle block height it not null") {

            beforeEach {
                whenever(merkleBlock.height).thenReturn(merkleHeight)
            }

            it("force adds merkle block") {
                blockSyncer.handleMerkleBlock(merkleBlock, maxBlockHeight)

                verify(storage).inTransaction(any())
                verify(blockchain).forceAdd(merkleBlock, merkleHeight, realm)
            }
        }

        context("when bloom filter expired while processing transaction") {
            beforeEach {
                whenever(transactionProcessor.process(merkleBlock.associatedTransactions, block, state.iterationHasPartialBlocks, realm))
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

                verify(storage).deleteBlockHash(block.reversedHeaderHashHex)
            }
        }
    }

    describe("#shouldRequest") {
        val block = mock(Block::class.java)
        val blockHash = byteArrayOf(1)

        it("returns true if block exists") {
            whenever(storage.getBlock(blockHash)).thenReturn(block)

            assertEquals(true, blockSyncer.shouldRequest(blockHash))
        }
    }

})
