package bitcoin.wallet.kit.headers

import bitcoin.wallet.kit.Fixtures
import junit.framework.Assert
import org.junit.Test

class DifficultyCalculatorTest {

    private val calculator = DifficultyCalculator()

    @Test
    fun difficultyAfter() {
        val newDifficulty = calculator.difficultyAfter(
                Fixtures.block3,
                Fixtures.checkpointBlock1
        )

        Assert.assertEquals(newDifficulty, Fixtures.checkpointBlock2.header?.bits)
    }

}
