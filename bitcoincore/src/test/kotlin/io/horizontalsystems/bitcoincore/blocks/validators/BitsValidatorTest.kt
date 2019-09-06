package io.horizontalsystems.bitcoincore.blocks.validators

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.models.Block
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object BitsValidatorTest : Spek({
    lateinit var validator: BitsValidator
    val block = mock<Block>()
    val previousBlock = mock<Block>()

    beforeEachTest {
        validator = BitsValidator()
    }

    describe("#validate") {

        it("passes when block and prev block bits are equal") {
            whenever(block.bits).thenReturn(1)
            whenever(previousBlock.bits).thenReturn(1)

            assertDoesNotThrow {
                validator.validate(block, previousBlock)
            }
        }

        it("fails when block and prev block bits are not equal") {
            whenever(block.bits).thenReturn(1)
            whenever(previousBlock.bits).thenReturn(2)

            assertThrows<BlockValidatorException.NotEqualBits> {
                validator.validate(block, previousBlock)
            }
        }

    }
})
