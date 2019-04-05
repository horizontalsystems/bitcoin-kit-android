package io.horizontalsystems.bitcoinkit.blocks.validators

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.crypto.CompactBits
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.network.Network

open class BitcoinCashValidator(private val network: Network, private val storage: IStorage) : BitcoinBlockValidator(network, storage) {
    private val largestHash = 1.toBigInteger() shl 256
    private val diffDate = 1510600000

    override fun validate(block: Block, previousBlock: Block) {
        if (medianTimePast(block) >= diffDate) {
            getPrevious(previousBlock, 147) ?: return

            validateDAA(block, previousBlock)
        } else if (isDifficultyTransitionEdge(block.height)) {
            checkDifficultyTransitions(block)
        } else {
            validateEDA(block, previousBlock)
        }
    }

    //  Get median of last 3 blocks based on timestamp
    fun getSuitableBlock(block: Block): Block {
        val blocks: MutableList<Block> = mutableListOf()
        blocks.add(block)
        blocks.add(blocks[0].previousBlock(storage) ?: throw BlockValidatorException.NoPreviousBlock())
        blocks.add(blocks[1].previousBlock(storage) ?: throw BlockValidatorException.NoPreviousBlock())

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

    private fun blockTimestamp(block: Block) = block.timestamp

    //  Difficulty adjustment algorithm
    open fun validateDAA(candidate: Block, previousBlock: Block) {

        val lstBlock = getSuitableBlock(previousBlock)
        val previous = checkNotNull(getPrevious(previousBlock, 144)) { throw BlockValidatorException.NoPreviousBlock() }
        val fstBLock = getSuitableBlock(previous)
        val heightInterval = lstBlock.height - fstBLock.height

        var actualTimespan = lstBlock.timestamp - fstBLock.timestamp
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
            val target = CompactBits.decode(it.bits)
            chainWork += largestHash / (target + 1.toBigInteger())
        }

        chainWork = chainWork * network.targetSpacing.toBigInteger() / actualTimespan.toBigInteger()

        val target = largestHash / chainWork - 1.toBigInteger()
        val bits = CompactBits.encode(target)
        if (bits != candidate.bits) {
            throw BlockValidatorException.NotEqualBits()
        }
    }

    //  Emergency Difficulty Adjustement
    fun validateEDA(candidate: Block, previousBlock: Block) {
        if (previousBlock.bits.toBigInteger() == network.maxTargetBits) {
            if (candidate.bits.toBigInteger() != network.maxTargetBits) {
                throw BlockValidatorException.NotEqualBits()
            }

            return
        }

        val cursorBlock = checkNotNull(getPrevious(previousBlock, 6)) {
            throw BlockValidatorException.NoPreviousBlock()
        }

        val mpt6blocks = medianTimePast(previousBlock) - medianTimePast(cursorBlock)
        if (mpt6blocks >= 12 * 3600) {
            val pow = CompactBits.decode(previousBlock.bits) shr 2
            var powBits = CompactBits.encode(pow).toBigInteger()
            if (powBits > network.maxTargetBits)
                powBits = network.maxTargetBits
            if (powBits != candidate.bits.toBigInteger()) {
                throw BlockValidatorException.NotEqualBits()
            }
        } else if (previousBlock.bits == candidate.bits) {
            throw BlockValidatorException.NotEqualBits()
        }
    }

    fun medianTimePast(block: Block): Long {
        val median = mutableListOf<Long>()
        var currentBlock = block

        for (i in 0 until 11) {
            median.add(currentBlock.timestamp)
            currentBlock = currentBlock.previousBlock(storage) ?: break
        }

        if (median.isEmpty()) {
            return currentBlock.timestamp
        }

        median.sort()
        return median[median.size / 2]
    }

}
