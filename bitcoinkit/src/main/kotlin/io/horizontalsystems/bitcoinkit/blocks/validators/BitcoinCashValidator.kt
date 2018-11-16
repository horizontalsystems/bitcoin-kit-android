package io.horizontalsystems.bitcoinkit.blocks.validators

import io.horizontalsystems.bitcoinkit.crypto.CompactBits
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.Header
import io.horizontalsystems.bitcoinkit.network.Network

open class BitcoinCashValidator(private val network: Network) : BlockValidator(network) {
    private val largestHash = 1.toBigInteger() shl 256
    private val diffDate = 1510600000

    override fun validate(candidate: Block, previousBlock: Block) {
        validateHeader(candidate, previousBlock)

        if (medianTimePast(candidate) >= diffDate) {
            getPrevious(previousBlock, 147) ?: return

            validateDAA(candidate, previousBlock)
        } else if (isDifficultyTransitionEdge(candidate.height)) {
            checkDifficultyTransitions(candidate)
        } else {
            validateEDA(candidate, previousBlock)
        }
    }

    //  Get median of last 3 blocks based on timestamp
    fun getSuitableBlock(block: Block): Block {
        val blocks: MutableList<Block> = mutableListOf()
        blocks.add(block)
        blocks.add(blocks[0].previousBlock ?: throw BlockValidatorException.NoPreviousBlock())
        blocks.add(blocks[1].previousBlock ?: throw BlockValidatorException.NoPreviousBlock())

        blocks.reverse()

        if (blockTimestamp(blocks[0]) > blockTimestamp(blocks[2])) {
            blocks.swap(0, 2)
        }

        if (blockTimestamp(blocks[0]) > blockTimestamp(blocks[1])) {
            blocks.swap(0, 1)
        }

        if (blockTimestamp(blocks[1]) > blockTimestamp(blocks[2])) {
            blocks.swap(1, 2)
        }

        return blocks[1]
    }

    private fun MutableList<Block>.swap(index1: Int, index2: Int) {
        val tmp = this[index1]
        this[index1] = this[index2]
        this[index2] = tmp
    }

    private fun blockTimestamp(block: Block) = block.header?.timestamp ?: 0

    //  Difficulty adjustment algorithm
    open fun validateDAA(candidate: Block, previousBlock: Block) {
        val candidateHeader = checkNotNull(candidate.header) {
            throw BlockValidatorException.NoHeader()
        }

        val lstBlock = getSuitableBlock(previousBlock)
        val previous = checkNotNull(getPrevious(previousBlock, 144)) { throw BlockValidatorException.NoPreviousBlock() }
        val fstBLock = getSuitableBlock(previous)
        val lstBlockHeader = lstBlock.header
        val fstBLockHeader = fstBLock.header
        val heightInterval = lstBlock.height - fstBLock.height

        if (lstBlockHeader == null || fstBLockHeader == null) {
            throw BlockValidatorException.NoHeader()
        }

        var actualTimespan = lstBlockHeader.timestamp - fstBLockHeader.timestamp
        if (actualTimespan > 288 * network.targetSpacing)
            actualTimespan = 288 * network.targetSpacing.toLong()
        if (actualTimespan < 72 * network.targetSpacing)
            actualTimespan = 72 * network.targetSpacing.toLong()

        var blocks = checkNotNull(getPreviousWindow(lstBlock, heightInterval - 1)) {
            throw BlockValidatorException.NoPreviousBlock()
        }

        var chainWork = 0.toBigInteger()
        blocks += lstBlock
        blocks.forEach {
            val target = CompactBits.decode(it.header!!.bits)
            chainWork += largestHash / (target + 1.toBigInteger())
        }

        chainWork = chainWork * network.targetSpacing.toBigInteger() / actualTimespan.toBigInteger()

        val target = largestHash / chainWork - 1.toBigInteger()
        val bits = CompactBits.encode(target)
        if (bits != candidateHeader.bits) {
            throw BlockValidatorException.NotEqualBits()
        }
    }

    //  Emergency Difficulty Adjustement
    fun validateEDA(candidate: Block, previousBlock: Block) {
        val candidateHeader = checkNotNull(candidate.header)
        val blockHeader = checkNotNull(previousBlock.header)

        if (blockHeader.bits.toBigInteger() == network.maxTargetBits) {
            if (candidateHeader.bits.toBigInteger() != network.maxTargetBits) {
                throw BlockValidatorException.NotEqualBits()
            }

            return
        }

        val cursorBlock = checkNotNull(getPrevious(previousBlock, 6)) {
            throw BlockValidatorException.NoPreviousBlock()
        }

        val mpt6blocks = medianTimePast(previousBlock) - medianTimePast(cursorBlock)
        if (mpt6blocks >= 12 * 3600) {
            val pow = CompactBits.decode(blockHeader.bits) shr 2
            var powBits = CompactBits.encode(pow).toBigInteger()
            if (powBits > network.maxTargetBits)
                powBits = network.maxTargetBits
            if (powBits != candidateHeader.bits.toBigInteger()) {
                throw BlockValidatorException.NotEqualBits()
            }
        } else if (blockHeader.bits == candidateHeader.bits) {
            throw BlockValidatorException.NotEqualBits()
        }
    }

    fun medianTimePast(block: Block): Long {
        val median = mutableListOf<Long>()
        var currentBlock = block

        var header: Header
        for (i in 0 until 11) {
            header = checkNotNull(currentBlock.header) { throw Exception() }
            median.add(header.timestamp)
            currentBlock = currentBlock.previousBlock ?: break
        }

        if (median.isEmpty()) {
            header = checkNotNull(currentBlock.header) { throw Exception() }
            return header.timestamp
        }

        median.sort()
        return median[median.size / 2]
    }

}
