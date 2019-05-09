package io.horizontalsystems.dashkit.validators

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoincore.managers.BlockValidatorHelper
import io.horizontalsystems.bitcoincore.models.Block
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class DarkGravityWaveValidatorTest : Spek({
    val bitsList = arrayOf(
            0x1b104be1, 0x1b10e09e, 0x1b11a33c, 0x1b121cf3, 0x1b11951e, 0x1b11abac, 0x1b118d9c,
            0x1b1123f9, 0x1b1141bf, 0x1b110764, 0x1b107556, 0x1b104297, 0x1b1063d0, 0x1b10e878,
            0x1b0dfaff, 0x1b0c9ab8, 0x1b0c03d6, 0x1b0dd168, 0x1b10b864, 0x1b0fed89, 0x1b113ff1,
            0x1b10460b, 0x1b13b83f, 0x1b1418d4)

    val timestampArray = arrayOf(
            1408728124, 1408728332, 1408728479, 1408728495, 1408728608, 1408728744, 1408728756,
            1408728950, 1408729116, 1408729179, 1408729305, 1408729474, 1408729576, 1408729587,
            1408729647, 1408729678, 1408730179, 1408730862, 1408730914, 1408731242, 1408731256,
            1408732229, 1408732257, 1408732489) // 123433 - 123456

    val blockHelper = mock<BlockValidatorHelper>()
    val candidateHeight = 25
    val heightInterval = 24

    val validator by memoized {
        DarkGravityWaveValidator(blockHelper, 24, 3600, 0x1e0fffff, 0)
    }

    describe("#validate") {
        context("when candidate bits are valid") {
            val candidate = mock<Block> {
                on { version } doReturn 1
                on { timestamp } doReturn 1408732505
                on { bits } doReturn 0x1b1441de
                on { nonce } doReturn 1
                on { height } doReturn candidateHeight
            }

            beforeEach {
                var lastBlock = candidate

                repeat(heightInterval) { i ->
                    val block = mock<Block> {
                        on { version } doReturn 1
                        on { timestamp } doReturn timestampArray[timestampArray.size - i - 1].toLong()
                        on { bits } doReturn bitsList[bitsList.size - i - 1].toLong()
                        on { nonce } doReturn 0
                        on { height } doReturn candidateHeight - i - 1
                    }

                    whenever(blockHelper.getPrevious(lastBlock, 1)).thenReturn(block)

                    lastBlock = block
                }
            }

            it("checks bits") {
                assertDoesNotThrow {
                    val prevBlock = blockHelper.getPrevious(candidate, 1)!!
                    validator.validate(candidate, prevBlock)
                }
            }
        }

        context("when there is no NoPreviousBlock") {
            val candidateBlock = mock<Block> {
                on { bits } doReturn 0x1000000f
            }

            val previousBlock = mock<Block> {
                on { height } doReturn 25
            }

            beforeEach {
                whenever(blockHelper.getPrevious(previousBlock, 1)).thenReturn(null)
            }

            it("throws an exception NoPreviousBlock") {
                assertThrows<BlockValidatorException.NoPreviousBlock> {
                    validator.validate(candidateBlock, previousBlock)
                }
            }
        }

    }
})
