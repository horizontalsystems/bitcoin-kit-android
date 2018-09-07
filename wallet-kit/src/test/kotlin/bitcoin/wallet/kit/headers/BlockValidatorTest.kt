package bitcoin.wallet.kit.headers

import bitcoin.wallet.kit.Fixtures
import bitcoin.wallet.kit.models.Block
import org.junit.Assert
import org.junit.Test

class BlockValidatorTest {
    private val validator = BlockValidator()

    @Test
    fun validate() {
        val block1 = Block().apply {
            height = 1
            header = Fixtures.block1.header
        }

        val block2 = Block().apply {
            header = Fixtures.block2.header
            previousBlock = block1
        }

        try {
            validator.validate(block2)
        } catch (e: Exception) {
            Assert.fail("BLock validation failed with: ${e.message}")
        }
    }
}
