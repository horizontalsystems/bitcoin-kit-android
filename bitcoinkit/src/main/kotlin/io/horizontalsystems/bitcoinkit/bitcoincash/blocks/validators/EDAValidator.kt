package io.horizontalsystems.bitcoinkit.bitcoincash.blocks.validators

import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoinkit.blocks.validators.IBlockValidator
import io.horizontalsystems.bitcoinkit.crypto.CompactBits
import io.horizontalsystems.bitcoinkit.managers.BlockHelper
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.network.Network

// Emergency Difficulty Adjustment
class EDAValidator(private val network: Network, private val blockHelper: BlockHelper) : IBlockValidator {

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return true
    }

    override fun validate(block: Block, previousBlock: Block) {
        if (previousBlock.bits.toBigInteger() == network.maxTargetBits) {
            if (block.bits.toBigInteger() != network.maxTargetBits) {
                throw BlockValidatorException.NotEqualBits()
            }

            return
        }

        val cursorBlock = checkNotNull(blockHelper.getPrevious(previousBlock, 6)) {
            throw BlockValidatorException.NoPreviousBlock()
        }

        val mpt6blocks = blockHelper.medianTimePast(previousBlock) - blockHelper.medianTimePast(cursorBlock)
        if (mpt6blocks >= 12 * 3600) {
            val pow = CompactBits.decode(previousBlock.bits) shr 2
            var powBits = CompactBits.encode(pow).toBigInteger()
            if (powBits > network.maxTargetBits)
                powBits = network.maxTargetBits
            if (powBits != block.bits.toBigInteger()) {
                throw BlockValidatorException.NotEqualBits()
            }
        } else if (previousBlock.bits != block.bits) {
            throw BlockValidatorException.NotEqualBits()
        }
    }
}
