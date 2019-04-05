package io.horizontalsystems.bitcoinkit.blocks.validators

import io.horizontalsystems.bitcoinkit.crypto.CompactBits
import io.horizontalsystems.bitcoinkit.models.Block
import java.math.BigInteger

class BlockHeaderValidator : IBlockValidator {

    override fun validate(block: Block, previousBlock: Block) {
        check(BigInteger(block.headerHashReversedHex, 16) < CompactBits.decode(block.bits)) {
            throw BlockValidatorException.InvalidProveOfWork()
        }

        check(block.previousBlockHash.contentEquals(previousBlock.headerHash)) {
            throw BlockValidatorException.WrongPreviousHeader()
        }
    }
}
