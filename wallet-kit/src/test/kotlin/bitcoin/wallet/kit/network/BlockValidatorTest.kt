package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.blocks.BlockValidator
import bitcoin.wallet.kit.blocks.BlockValidatorException
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.models.Header
import helpers.Fixtures
import org.junit.Assert.fail
import org.junit.Test

class BlockValidatorTest {

    @Test
    fun validateHeader() {
        val block1 = Block(Fixtures.block1.header!!, 1)
        val block2 = Block(Fixtures.block2.header!!, block1)

        try {
            BlockValidator.validateHeader(block2, block1)
        } catch (e: BlockValidatorException) {
            fail("Header validation failed with: ${e.message}")
        }
    }

    @Test
    fun validateHeader_invalidHeader() {
        try {
            BlockValidator.validateHeader(Block(), Block())
            fail("Expected exception: NoHeader")
        } catch (e: BlockValidatorException.NoHeader) {
        }

        val block1 = Block().apply { }
        val block2 = Block(Fixtures.block2.header!!, block1)

        try {
            BlockValidator.validateHeader(block2, block1)
            fail("Expected exception: WrongPreviousHeader")
        } catch (e: BlockValidatorException.WrongPreviousHeader) {
        }
    }

    @Test
    fun validateBits() {
        val block1 = Block(Fixtures.block1.header!!, 1)
        val block2 = Block(Fixtures.block2.header!!, block1)

        try {
            BlockValidator.validateBits(block2, block1)
        } catch (e: BlockValidatorException) {
            fail("Bits validation failed with: ${e.message}")
        }
    }

    @Test
    fun validateHeader_invalidBits() {
        try {
            BlockValidator.validateBits(Block(), Block())
            fail("Expected exception: NoHeader")
        } catch (e: BlockValidatorException.NoHeader) {
        }

        val block1 = Block(Header().apply { bits = 1 }, 1)
        val block2 = Block(Fixtures.block2.header!!, Block())

        try {
            BlockValidator.validateBits(block2, block1)
            fail("Expected exception: NotEqualBits")
        } catch (e: BlockValidatorException.NotEqualBits) {

        }
    }

}
