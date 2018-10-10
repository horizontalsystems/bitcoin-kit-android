package bitcoin.wallet.kit.blocks.validators

import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.models.Header
import bitcoin.wallet.kit.network.MainNet
import bitcoin.walllet.kit.utils.HashUtils
import helpers.Fixtures
import org.junit.Assert.fail
import org.junit.Test

class MainnetValidatorTest {
    private val network = MainNet()
    private val validator = MainnetValidator(network)

    @Test
    fun checkDifficultyTransitions() {
        val check1 = Fixtures.checkpointBlock1

        var checkPrev = check1
        val prevsHead = Header().apply {
            version = 536870912
            prevHash = HashUtils.toBytesAsLE("000000000000000000124a73e879fd66a1b29d1b4b3f1a81de3cbcbe579e21a8")
            merkleHash = HashUtils.toBytesAsLE("7904930640df999005df3b57f9c6f542088af33c3d773dcec2939f55ced359b8")
            timestamp = 1535129301
            bits = 388763047
            nonce = 59591417
        }

        for (i in 1 until 2016) {
            checkPrev = Block(prevsHead, checkPrev)
        }

        val check2Head = Header().apply {
            version = 536870912
            prevHash = HashUtils.toBytesAsLE("0000000000000000001d9d48d93793aaa85b5f6d17c176d4ef905c7e7112b1cf")
            merkleHash = HashUtils.toBytesAsLE("3ad0fa0e8c100db5831ebea7cabf6addae2c372e6e1d84f6243555df5bbfa351")
            timestamp = 1535129431
            bits = 388618029
            nonce = 2367954839
        }

        val check2 = Block(check2Head, checkPrev)

        try {
            validator.checkDifficultyTransitions(check2)
        } catch (e: Exception) {
            fail(e.message)
        }
    }

}
