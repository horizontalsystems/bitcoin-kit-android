package io.horizontalsystems.bitcoincash.blocks.validators

import io.horizontalsystems.bitcoincash.blocks.BitcoinCashBlockValidatorHelper
import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoincore.blocks.validators.IBlockValidator
import io.horizontalsystems.bitcoincore.crypto.CompactBits
import io.horizontalsystems.bitcoincore.models.Block

// Emergency Difficulty Adjustment
class EDAValidator(private val maxTargetBits: Long, private val blockValidatorHelper: BitcoinCashBlockValidatorHelper, val firstCheckpointHeight: Int) : IBlockValidator {

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return true
    }

    override fun validate(block: Block, previousBlock: Block) {
        // we must trust first 6 blocks from checkpoint, because can't calculate it's bits
        if (previousBlock.height < firstCheckpointHeight + 6) return

        if (previousBlock.bits == maxTargetBits) {
            if (block.bits != maxTargetBits) {
                throw BlockValidatorException.NotEqualBits()
            }

            return
        }

        val cursorBlock = checkNotNull(blockValidatorHelper.getPrevious(previousBlock, 6)) {
            throw BlockValidatorException.NoPreviousBlock()
        }

        val mpt6blocks = blockValidatorHelper.medianTimePast(previousBlock) - blockValidatorHelper.medianTimePast(cursorBlock)
        if (mpt6blocks >= 12 * 3600) {
            val decodedBits = CompactBits.decode(previousBlock.bits)
            val pow = decodedBits + (decodedBits shr 2)
            var powBits = CompactBits.encode(pow)
            if (powBits > maxTargetBits)
                powBits = maxTargetBits
            if (powBits != block.bits) {
                throw BlockValidatorException.NotEqualBits()
            }
        } else if (previousBlock.bits != block.bits) {
            throw BlockValidatorException.NotEqualBits()
        }
    }
}
