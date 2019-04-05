package io.horizontalsystems.bitcoinkit.blocks.validators

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.network.Network

class BitcoinTestnetValidator(private val network: Network, private val storage: IStorage) : BitcoinBlockValidator(network, storage) {
    private val diffDate = 1329264000L // February 16th 2012

    override fun validate(block: Block, previousBlock: Block) {
        if (isDifficultyTransitionEdge(block.height)) {
            checkDifficultyTransitions(block)
        } else if (previousBlock.timestamp > diffDate) {
            validateDifficulty(block, previousBlock)
        } else {
            validateBits(block, previousBlock)
        }
    }

    private fun validateDifficulty(block: Block, previousBlock: Block) {

        val timeDelta = block.timestamp - previousBlock.timestamp
        if (timeDelta >= 0 && timeDelta <= network.targetSpacing * 2) {
            var cursor = block

            while (cursor.height != 0 && (cursor.height % network.heightInterval.toInt()) != 0 && cursor.bits == network.maxTargetBits.toLong()) {
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
