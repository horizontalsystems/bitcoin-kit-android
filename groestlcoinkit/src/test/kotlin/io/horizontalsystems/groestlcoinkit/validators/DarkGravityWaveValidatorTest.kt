package io.horizontalsystems.groestlcoinkit.validators

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
            0x1c024ef0, 0x1c021b2b, 0x1c0218df, 0x1c0210d6, 0x1c01f7d9, 0x1c01ec26, 0x1c01df18,
            0x1c01dba2, 0x1c01d847, 0x1c01ddf3, 0x1c01c348, 0x1c01c5c5, 0x1c01c487, 0x1c021a4c,
            0x1c025175, 0x1c022bdb, 0x1c021247, 0x1c020365, 0x1c020152, 0x1c021973, 0x1c020478,
            0x1c01f628, 0x1c01f1ed, 0x1c01df3d)

    val timestampArray = arrayOf(
            1402076876, 1402076903, 1402076931, 1402076955, 1402076998, 1402077009, 1402077023,
            1402077194, 1402077232, 1402077237, 1402077353, 1402077360, 1402077661, 1402077832,
            1402077848, 1402077850, 1402077857, 1402077965, 1402078026, 1402078078, 1402078115,
            1402078175, 1402078207, 1402078212) // 123433 - 123456

    val blockHelper = mock<BlockValidatorHelper>()
    val candidateHeight = 25
    val heightInterval = 24

    val validator by memoized {
        DarkGravityWaveValidator(blockHelper, 24, 1440, 0x1e0fffff, 0, 100000)
    }

    describe("#validate") {
        context("when candidate bits are valid") {
            val candidate = mock<Block> {
                on { version } doReturn 112
                on { timestamp } doReturn 1402078229
                on { bits } doReturn 0x1c01d7cf
                on { nonce } doReturn 33393923
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
