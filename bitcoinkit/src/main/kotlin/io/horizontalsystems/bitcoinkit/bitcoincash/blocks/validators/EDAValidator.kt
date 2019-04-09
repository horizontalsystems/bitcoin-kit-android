package io.horizontalsystems.bitcoinkit.bitcoincash.blocks.validators

import io.horizontalsystems.bitcoinkit.bitcoincash.blocks.BitcoinCashBlockValidatorHelper
import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoinkit.blocks.validators.IBlockValidator
import io.horizontalsystems.bitcoinkit.crypto.CompactBits
import io.horizontalsystems.bitcoinkit.models.Block
import java.math.BigInteger

// Emergency Difficulty Adjustment
class EDAValidator(private val maxTargetBits: BigInteger, private val blockValidatorHelper: BitcoinCashBlockValidatorHelper) : IBlockValidator {

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return true
    }

    override fun validate(block: Block, previousBlock: Block) {
        if (previousBlock.bits.toBigInteger() == maxTargetBits) {
            if (block.bits.toBigInteger() != maxTargetBits) {
                throw BlockValidatorException.NotEqualBits()
            }

            return
        }

        val cursorBlock = checkNotNull(blockValidatorHelper.getPrevious(previousBlock, 6)) {
            throw BlockValidatorException.NoPreviousBlock()
        }

        val mpt6blocks = blockValidatorHelper.medianTimePast(previousBlock) - blockValidatorHelper.medianTimePast(cursorBlock)
        if (mpt6blocks >= 12 * 3600) {
            val pow = CompactBits.decode(previousBlock.bits) shr 2
            var powBits = CompactBits.encode(pow).toBigInteger()
            if (powBits > maxTargetBits)
                powBits = maxTargetBits
            if (powBits != block.bits.toBigInteger()) {
                throw BlockValidatorException.NotEqualBits()
            }
        } else if (previousBlock.bits != block.bits) {
            throw BlockValidatorException.NotEqualBits()
        }
    }
}
