package io.horizontalsystems.bitcoinkit.blocks.validators

import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.network.Network

class TestnetValidator(private val network: Network) : BlockValidator(network) {
    private val diffDate = 1329264000L // February 16th 2012

    override fun validate(candidate: Block, previousBlock: Block) {
        val previousBlockHeader = checkNotNull(previousBlock.header) {
            throw BlockValidatorException.NoHeader()
        }

        validateHeader(candidate, previousBlock)

        if (isDifficultyTransitionEdge(candidate.height)) {
            checkDifficultyTransitions(candidate)
        } else if (previousBlockHeader.timestamp > diffDate) {
            validateDifficulty(candidate, previousBlock)
        } else {
            validateBits(candidate, previousBlock)
        }
    }

    private fun validateDifficulty(block: Block, previousBlock: Block) {
        val previousBlockHeader = checkNotNull(previousBlock.header) {
            throw BlockValidatorException.NoHeader()
        }

        val blockHeader = checkNotNull(block.header) {
            throw BlockValidatorException.NoHeader()
        }

        val timeDelta = blockHeader.timestamp - previousBlockHeader.timestamp
        if (timeDelta >= 0 && timeDelta <= network.targetSpacing * 2) {
            var cursor = block
            var cursorHeader = checkNotNull(cursor.header)

            while (cursor.height != 0 && (cursor.height % network.heightInterval.toInt()) != 0 && cursorHeader.bits == network.maxTargetBits.toLong()) {
                val prevBlock = checkNotNull(cursor.previousBlock) {
                    throw BlockValidatorException.NoPreviousBlock()
                }

                val header = checkNotNull(prevBlock.header) {
                    throw BlockValidatorException.NoHeader()
                }

                cursor = prevBlock
                cursorHeader = header
            }

            if (cursorHeader.bits != blockHeader.bits) {
                BlockValidatorException.NotEqualBits()
            }
        }
    }
}
