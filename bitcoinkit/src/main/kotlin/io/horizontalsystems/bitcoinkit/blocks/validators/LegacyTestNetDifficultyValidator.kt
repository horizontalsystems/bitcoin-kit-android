package io.horizontalsystems.bitcoinkit.blocks.validators

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.network.Network

class LegacyTestNetDifficultyValidator(private val network: Network, private val storage: IStorage) : IBlockValidator {
    private val diffDate = 1329264000 // February 16th 2012

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return previousBlock.timestamp > diffDate
    }

    override fun validate(block: Block, previousBlock: Block) {
        val timeDelta = block.timestamp - previousBlock.timestamp
        if (timeDelta >= 0 && timeDelta <= network.targetSpacing * 2) {
            var cursor = block

            while (cursor.height % network.heightInterval != 0L && cursor.bits == network.maxTargetBits) {
                val prevBlock = checkNotNull(cursor.previousBlock(storage)) {
                    throw BlockValidatorException.NoPreviousBlock()
                }

                cursor = prevBlock
            }

            if (cursor.bits != block.bits) {
                BlockValidatorException.NotEqualBits()
            }
        }
    }
}
