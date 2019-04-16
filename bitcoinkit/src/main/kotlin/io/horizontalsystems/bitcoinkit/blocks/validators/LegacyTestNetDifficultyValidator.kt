package io.horizontalsystems.bitcoinkit.blocks.validators

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.models.Block

class LegacyTestNetDifficultyValidator(
        private val storage: IStorage,
        private val heightInterval: Long,
        private val targetSpacing: Int,
        private val maxTargetBits: Long)
    : IBlockValidator {

    private val diffDate = 1329264000 // February 16th 2012

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return previousBlock.timestamp > diffDate
    }

    override fun validate(block: Block, previousBlock: Block) {
        val timeDelta = block.timestamp - previousBlock.timestamp
        if (timeDelta >= 0 && timeDelta <= targetSpacing * 2) {
            var cursor = block

            while (cursor.height % heightInterval != 0L && cursor.bits == maxTargetBits) {
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
