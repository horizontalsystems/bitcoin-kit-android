package io.horizontalsystems.bitcoincash.blocks.validators

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoincore.blocks.validators.IBlockValidator
import io.horizontalsystems.bitcoincore.models.Block
import org.junit.Assert
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertThrows
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ForkValidatorTest : Spek({
    val forkHeight = 100
    val expectedBlockHash = byteArrayOf(1, 2, 3)
    val concreteBlockValidator = mock<IBlockValidator>()
    val validator by memoized { ForkValidator(forkHeight, expectedBlockHash, concreteBlockValidator) }

    describe("#isBlockValidatable") {
        val block = mock<Block>()

        it("is true when block height is equal to fork height") {
            whenever(block.height).thenReturn(forkHeight)
            Assert.assertTrue(validator.isBlockValidatable(block, mock()))
        }

        it("is false when block height is not equal to fork height") {
            whenever(block.height).thenReturn(104)
            Assert.assertFalse(validator.isBlockValidatable(block, mock()))
        }

    }

    describe("#validate") {
        val block = mock<Block>()

        it("validates without any error when block hash is equal to expected block hash") {
            whenever(block.headerHash).thenReturn(expectedBlockHash)
            Assertions.assertDoesNotThrow {
                validator.validate(block, mock())
            }
        }

        it("throws exception when block hash is not equal to expected block hash") {
            whenever(block.headerHash).thenReturn(byteArrayOf(3, 2, 1))
            assertThrows<BlockValidatorException.WrongBlockHash> {
                validator.validate(block, mock())
            }
        }
    }

})
