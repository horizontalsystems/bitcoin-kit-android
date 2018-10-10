package bitcoin.wallet.kit.blocks.validators

import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.network.NetworkParameters

class TestnetValidator(network: NetworkParameters) : MainnetValidator(network) {
    private val diffDate = 1329264000L // February 16th 2012

    override fun validate(block: Block, previousBlock: Block) {
        validateHeader(block, previousBlock)

        if (isDifficultyTransitionEdge(block.height)) {
            checkDifficultyTransitions(block)
        } else {
            validateBits(block, previousBlock)
        }
    }

    override fun checkDifficultyTransitions(block: Block) {
        var previousBlock = checkNotNull(block.previousBlock) { throw BlockValidatorException.NoPreviousBlock() }
        val previousBlockHeader = checkNotNull(previousBlock.header) {
            throw BlockValidatorException.NoHeader()
        }

        if (previousBlockHeader.timestamp > diffDate) {
            val blockHeader = checkNotNull(block.header) {
                throw BlockValidatorException.NoHeader()
            }

            val timeDelta = blockHeader.timestamp - previousBlockHeader.timestamp
            if (timeDelta >= 0 && timeDelta <= network.targetSpacing * 2) {
                var cursor = block
                var cursorHeader = checkNotNull(cursor.header)


                while (cursor.height != 0 && (cursor.height % network.heightInterval.toInt()) != 0 && cursorHeader.bits == network.maxTargetBits.toLong()) {
                    previousBlock = checkNotNull(cursor.previousBlock) {
                        throw BlockValidatorException.NoPreviousBlock()
                    }

                    val header = checkNotNull(previousBlock.header) {
                        throw BlockValidatorException.NoHeader()
                    }

                    cursor = previousBlock
                    cursorHeader = header
                }

                if (cursorHeader.bits != blockHeader.bits) {
                    BlockValidatorException.NotEqualBits()
                }
            }
        } else super.checkDifficultyTransitions(block)
    }
}
