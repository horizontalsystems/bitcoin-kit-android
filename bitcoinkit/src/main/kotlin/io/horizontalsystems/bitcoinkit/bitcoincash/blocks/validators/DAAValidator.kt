package io.horizontalsystems.bitcoinkit.bitcoincash.blocks.validators

import io.horizontalsystems.bitcoinkit.bitcoincash.blocks.BitcoinCashBlockValidatorHelper
import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoinkit.blocks.validators.IBlockValidator
import io.horizontalsystems.bitcoinkit.crypto.CompactBits
import io.horizontalsystems.bitcoinkit.models.Block

class DAAValidator(private val targetSpacing: Int, private val blockValidatorHelper: BitcoinCashBlockValidatorHelper) : IBlockValidator {
    private val largestHash = 1.toBigInteger() shl 256
    private val diffDate = 1510600000 // 2017 November 3, 14:06 GMT

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return blockValidatorHelper.medianTimePast(block) >= diffDate
    }

    override fun validate(block: Block, previousBlock: Block) {
        blockValidatorHelper.getPrevious(previousBlock, 147) ?: return

        validateDAA(block, previousBlock)
    }

    private fun validateDAA(candidate: Block, previousBlock: Block) {
        val lstBlock = blockValidatorHelper.getSuitableBlock(previousBlock)
        val previous = checkNotNull(blockValidatorHelper.getPrevious(previousBlock, 144)) { throw BlockValidatorException.NoPreviousBlock() }
        val fstBLock = blockValidatorHelper.getSuitableBlock(previous)
        val heightInterval = lstBlock.height - fstBLock.height

        var actualTimespan = lstBlock.timestamp - fstBLock.timestamp
        if (actualTimespan > 288 * targetSpacing)
            actualTimespan = 288 * targetSpacing.toLong()
        if (actualTimespan < 72 * targetSpacing)
            actualTimespan = 72 * targetSpacing.toLong()

        var blocks = checkNotNull(blockValidatorHelper.getPreviousWindow(lstBlock, heightInterval - 1)) {
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
}
