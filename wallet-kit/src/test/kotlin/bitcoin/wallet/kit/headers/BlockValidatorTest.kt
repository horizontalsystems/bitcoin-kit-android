package bitcoin.wallet.kit.headers

import bitcoin.wallet.kit.models.Block
import helpers.Fixtures
import org.junit.Assert
import org.junit.Test

class BlockValidatorTest {
    private val validator = BlockValidator()

    @Test
    fun validate() {
        val block1 = Block(Fixtures.block1.header!!, 1)

        val block2 = Block(Fixtures.block2.header!!, block1)

        try {
            validator.validate(block2)
        } catch (e: Exception) {
            Assert.fail("BLock validation failed with: ${e.message}")
        }
    }
}
