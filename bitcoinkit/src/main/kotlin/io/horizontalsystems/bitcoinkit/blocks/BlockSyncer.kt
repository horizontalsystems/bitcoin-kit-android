package io.horizontalsystems.bitcoinkit.blocks

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.core.ISyncStateListener
import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.managers.AddressManager
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.bitcoinkit.transactions.TransactionProcessor

class BlockSyncer(
        private val storage: IStorage,
        private val blockchain: Blockchain,
        private val transactionProcessor: TransactionProcessor,
        private val addressManager: AddressManager,
        private val bloomFilterManager: BloomFilterManager,
        private val listener: ISyncStateListener,
        private val network: Network,
        private val state: State = State()) {

    private val sqliteMaxVariableNumber = 999

    val localDownloadedBestBlockHeight: Int
        get() = storage.lastBlock()?.height ?: 0

    val localKnownBestBlockHeight: Int
        get() {
            val blockHashes = storage.getBlockchainBlockHashes()
            val headerHexes = blockHashes.map { it.headerHashReversedHex }
            val existingBlocksCount = headerHexes.chunked(sqliteMaxVariableNumber).map {
                storage.blocksCount(it)
            }.sum()

            return localDownloadedBestBlockHeight.plus(blockHashes.size - existingBlocksCount)
        }

    init {
        if (storage.blocksCount() == 0) {
            storage.saveBlock(network.checkpointBlock)
        }

        listener.onInitialBestBlockHeightUpdate(localDownloadedBestBlockHeight)
    }

    fun prepareForDownload() {
        handlePartialBlocks()

        clearPartialBlocks()
        clearBlockHashes() // we need to clear block hashes when "syncPeer" is disconnected

        blockchain.handleFork()
    }

    fun downloadStarted() {
    }

    fun downloadIterationCompleted() {
        if (state.iterationHasPartialBlocks) {
            handlePartialBlocks()
        }
    }

    fun downloadCompleted() {
        blockchain.handleFork()
    }

    fun downloadFailed() {
        prepareForDownload()
    }

    fun getBlockHashes(): List<BlockHash> {
        return storage.getBlockHashesSortedBySequenceAndHeight(limit = 500)
    }

    fun getBlockLocatorHashes(peerLastBlockHeight: Int): List<ByteArray> {
        val result = mutableListOf<ByteArray>()

        storage.getLastBlockchainBlockHash()?.headerHash?.let {
            result.add(it)
        }

        if (result.isEmpty()) {
            storage.getBlocks(heightGreaterThan = network.checkpointBlock.height, sortedBy = "height", limit = 10).forEach {
                result.add(it.headerHash)
            }
        }

        val lastBlock = storage.getBlock(peerLastBlockHeight)
        if (lastBlock == null) {
            result.add(network.checkpointBlock.headerHash)
        } else if (!result.contains(lastBlock.headerHash)) {
            result.add(lastBlock.headerHash)
        }

        return result
    }

    fun addBlockHashes(blockHashes: List<ByteArray>) {
        var lastSequence = storage.getLastBlockHash()?.sequence ?: 0

        val existingHashes = storage.getBlockHashHeaderHashes()
        val newBlockHashes = blockHashes.filter { existingHashes.none { n -> n.contentEquals(it) } }.map {
            BlockHash(it, 0, ++lastSequence)
        }

        storage.addBlockHashes(newBlockHashes)
    }

    fun handleMerkleBlock(merkleBlock: MerkleBlock, maxBlockHeight: Int) {
        storage.inTransaction {
            val height = merkleBlock.height

            val block = when (height) {
                null -> blockchain.connect(merkleBlock)
                else -> blockchain.forceAdd(merkleBlock, height)
            }

            try {
                transactionProcessor.processIncoming(merkleBlock.associatedTransactions, block, state.iterationHasPartialBlocks)
            } catch (e: BloomFilterManager.BloomFilterExpired) {
                state.iterationHasPartialBlocks = true
            }

            if (!state.iterationHasPartialBlocks) {
                storage.deleteBlockHash(block.headerHashReversedHex)
            }

            listener.onCurrentBestBlockHeightUpdate(block.height, maxBlockHeight)
        }

    }

    fun shouldRequest(blockHash: ByteArray): Boolean {
        val hashHex = blockHash.reversedArray().toHexString()

        return storage.getBlock(hashHex) == null
    }

    private fun clearPartialBlocks() {
        val toDelete = storage.getBlockHashHeaderHashHexes(except = network.checkpointBlock.headerHashReversedHex)

        storage.inTransaction {
            toDelete.chunked(sqliteMaxVariableNumber).forEach {
                val blocksToDelete = storage.getBlocks(hashHexes = it)
                blockchain.deleteBlocks(blocksToDelete)
            }
        }
    }

    private fun handlePartialBlocks() {
        addressManager.fillGap()
        bloomFilterManager.regenerateBloomFilter()
        state.iterationHasPartialBlocks = false
    }

    private fun clearBlockHashes() {
        storage.deleteBlockchainBlockHashes()
    }

    class State(var iterationHasPartialBlocks: Boolean = false)
}
