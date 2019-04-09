package io.horizontalsystems.bitcoinkit.bitcoincash.blocks.validators

import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoinkit.blocks.validators.IBlockValidator
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.crypto.CompactBits
import io.horizontalsystems.bitcoinkit.managers.BlockHelper
import io.horizontalsystems.bitcoinkit.models.Block

class DAAValidator(private val targetSpacing: Int, private val storage: IStorage, private val blockHelper: BlockHelper) : IBlockValidator {
    private val largestHash = 1.toBigInteger() shl 256
    private val diffDate = 1510600000 // 2017 November 3, 14:06 GMT

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return blockHelper.medianTimePast(block) >= diffDate
    }

    override fun validate(block: Block, previousBlock: Block) {
        blockHelper.getPrevious(previousBlock, 147) ?: return

        validateDAA(block, previousBlock)
    }

    private fun validateDAA(candidate: Block, previousBlock: Block) {
        val lstBlock = getSuitableBlock(previousBlock)
        val previous = checkNotNull(blockHelper.getPrevious(previousBlock, 144)) { throw BlockValidatorException.NoPreviousBlock() }
        val fstBLock = getSuitableBlock(previous)
        val heightInterval = lstBlock.height - fstBLock.height

        var actualTimespan = lstBlock.timestamp - fstBLock.timestamp
        if (actualTimespan > 288 * targetSpacing)
            actualTimespan = 288 * targetSpacing.toLong()
        if (actualTimespan < 72 * targetSpacing)
            actualTimespan = 72 * targetSpacing.toLong()

        var blocks = checkNotNull(blockHelper.getPreviousWindow(lstBlock, heightInterval - 1)) {
            throw BlockValidatorException.NoPreviousBlock()
        }

        var chainWork = 0.toBigInteger()
        blocks += lstBlock
        blocks.forEach {
            val target = CompactBits.decode(it.bits)
            chainWork += largestHash / (target + 1.toBigInteger())
        }

        chainWork = chainWork * targetSpacing.toBigInteger() / actualTimespan.toBigInteger()

        val target = largestHash / chainWork - 1.toBigInteger()
        val bits = CompactBits.encode(target)
        if (bits != candidate.bits) {
            throw BlockValidatorException.NotEqualBits()
        }
    }

    private fun MutableList<Block>.swap(index1: Int, index2: Int) {
        val tmp = this[index1]
        this[index1] = this[index2]
        this[index2] = tmp
    }

    //  Get median of last 3 blocks based on timestamp
    private fun getSuitableBlock(block: Block): Block {
        val blocks: MutableList<Block> = mutableListOf()
        blocks.add(block)
        blocks.add(blocks[0].previousBlock(storage) ?: throw BlockValidatorException.NoPreviousBlock())
        blocks.add(blocks[1].previousBlock(storage) ?: throw BlockValidatorException.NoPreviousBlock())

        blocks.reverse()

        if (blocks[0].timestamp > blocks[2].timestamp) {
            blocks.swap(0, 2)
        }

        if (blocks[0].timestamp > blocks[1].timestamp) {
            blocks.swap(0, 1)
        }

        if (blocks[1].timestamp > blocks[2].timestamp) {
            blocks.swap(1, 2)
        }

        return blocks[1]
    }

}