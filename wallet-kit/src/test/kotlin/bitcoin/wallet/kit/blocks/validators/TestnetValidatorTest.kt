package bitcoin.wallet.kit.blocks.validators

import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.models.Header
import bitcoin.wallet.kit.network.TestNet
import bitcoin.walllet.kit.utils.HashUtils
import helpers.Fixtures
import org.junit.Assert
import org.junit.Test

class TestnetValidatorTest {
    private val network = TestNet()
    private val validator = TestnetValidator(network)

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
            Assert.fail(e.message)
        }
    }

    @Test
    fun checkDifficultyTransitions_afterDiffDate() {
        val check1 = Block().apply {
            height = 1411200
            header = Header().apply {
                version = 536870912
                prevHash = HashUtils.toBytesAsLE("00000000000000a5bf9029aebb1956200304ffee31bc09f1323ae412d81fa2b2")
                merkleHash = HashUtils.toBytesAsLE("dff076f1f3468f86785b42c10e6f23c849ccbc1d40a0fa8909b20b20fb204de2")
                timestamp = 1535560970
                bits = 424329477
                nonce = 2681700833
            }
        }

        var checkPrev = check1
        val prevsHead = Header().apply {
            version = 536870912
            prevHash = HashUtils.toBytesAsLE("000000000000003e3b50c7edca7bf59075b3d39ee2668076aa1ebe559787ff25")
            merkleHash = HashUtils.toBytesAsLE("6a05a10911d844e86e7758bf27ce183b2eaa5768108d992efdb6487c8f3f6dae")
            timestamp = 1536796796
            bits = 424329477
            nonce = 915088888
        }

        for (i in 1 until 2016) {
            checkPrev = Block(prevsHead, checkPrev)
        }

        val check2Head = Header().apply {
            version = 536870912
            prevHash = HashUtils.toBytesAsLE("0000000000000046f38ada53de3346d8191f69c8f3c0ba9e1950f5bf291989c4")
            merkleHash = HashUtils.toBytesAsLE("827bc2d47a164b9144a507eebc40b32f8f3e7e8c784b17e0a1fa245bfe9c100c")
            timestamp = 1536797113
            bits = 424435696
            nonce = 1267362056
        }

        val check2 = Block(check2Head, checkPrev)

        try {
            validator.checkDifficultyTransitions(check2)
        } catch (e: Exception) {
            Assert.fail(e.message)
        }
    }

}
