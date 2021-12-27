package com.anwang.safewallet.safekit

import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoincore.blocks.validators.IBlockChainedValidator
import io.horizontalsystems.bitcoincore.crypto.CompactBits
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.models.Block
import java.math.BigInteger

class SafeSPOSBlockValidator  : IBlockChainedValidator {

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return true
    }

    override fun validate(block: Block, previousBlock: Block) {
//        check(BigInteger(block.headerHash.toReversedHex(), 16) < CompactBits.decode(block.bits)) {
//            throw BlockValidatorException.InvalidProofOfWork()
//        }
    }
}