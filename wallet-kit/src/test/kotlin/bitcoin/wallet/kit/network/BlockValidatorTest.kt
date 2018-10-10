package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.blocks.validators.BlockValidator
import bitcoin.wallet.kit.blocks.validators.BlockValidatorException
import bitcoin.wallet.kit.blocks.validators.MainnetValidator
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.models.Header
import helpers.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class BlockValidatorTest {
    private val network = MainNet()
    private lateinit var validator: BlockValidator

    @Before
    fun setup() {
        validator = MainnetValidator(network)
    }

    @Test
    fun validateHeader() {
        val block1 = Block(Fixtures.block1.header!!, 1)
        val block2 = Block(Fixtures.block2.header!!, block1)

        try {
            validator.validateHeader(block2, block1)
        } catch (e: BlockValidatorException) {
            fail("Header validation failed with: ${e.message}")
        }
    }

    @Test
    fun validateHeader_invalidHeader() {
        try {
            validator.validateHeader(Block(), Block())
            fail("Expected exception: NoHeader")
        } catch (e: BlockValidatorException.NoHeader) {
        }

        val block1 = Block().apply { }
        val block2 = Block(Fixtures.block2.header!!, block1)

        try {
            validator.validateHeader(block2, block1)
            fail("Expected exception: WrongPreviousHeader")
        } catch (e: BlockValidatorException.WrongPreviousHeader) {
        }
    }

    @Test
    fun validateBits() {
        val block1 = Block(Fixtures.block1.header!!, 1)
        val block2 = Block(Fixtures.block2.header!!, block1)

        try {
            validator.validateBits(block2, block1)
        } catch (e: BlockValidatorException) {
            fail("Bits validation failed with: ${e.message}")
        }
    }

    @Test
    fun validateHeader_invalidBits() {
        try {
            validator.validateBits(Block(), Block())
            fail("Expected exception: NoHeader")
        } catch (e: BlockValidatorException.NoHeader) {
        }

        val block1 = Block(Header().apply { bits = 1 }, 1)
        val block2 = Block(Fixtures.block2.header!!, Block())

        try {
            validator.validateBits(block2, block1)
            fail("Expected exception: NotEqualBits")
        } catch (e: BlockValidatorException.NotEqualBits) {

        }
    }

    @Test
    fun difficultyTransitionPoint() {
        assertEquals(validator.isDifficultyTransitionEdge(0), true)
        assertEquals(validator.isDifficultyTransitionEdge(2015), false)
        assertEquals(validator.isDifficultyTransitionEdge(2016), true)
        assertEquals(validator.isDifficultyTransitionEdge(4032), true)
    }
}
