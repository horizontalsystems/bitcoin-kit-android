package bitcoin.wallet.kit.scripts

import bitcoin.wallet.kit.core.hexStringToByteArray
import bitcoin.wallet.kit.network.TestNet
import bitcoin.walllet.kit.hdwallet.Address
import org.junit.Assert.assertEquals
import org.junit.Test

class ScriptTest {
    private val sigScript = "47304402202b4da291cc39faf8433911988f9f49fc5c995812ca2f94db61468839c228c3e90220628bff3ff32ec95825092fa051cba28558a981fcf59ce184b14f2e215e69106701410414b38f4be3bb9fa0f4f32b74af07152b2f2f630bc02122a491137b6c523e46f18a0d5034418966f93dfc37cc3739ef7b2007213a302b7fba161557f4ad644a1c"
    private val pubkeyScript = "76a91433e81a941e64cda12c6a299ed322ddbdd03f8d0e88ac"

    private val network = TestNet()

    @Test
    fun scriptSig() {
        val sigProgBytes = sigScript.hexStringToByteArray()
        val script = Script(sigProgBytes)

        assertEquals("PUSHDATA(71)[304402202b4da291cc39faf8433911988f9f49fc5c995812ca2f94db61468839c228c3e90220628bff3ff32ec95825092fa051cba28558a981fcf59ce184b14f2e215e69106701] PUSHDATA(65)[0414b38f4be3bb9fa0f4f32b74af07152b2f2f630bc02122a491137b6c523e46f18a0d5034418966f93dfc37cc3739ef7b2007213a302b7fba161557f4ad644a1c]", script.toString())
    }

    @Test
    fun scriptPubKey() {
        val pubkeyBytes = pubkeyScript.hexStringToByteArray()
        val script = Script(pubkeyBytes)

        assertEquals("DUP HASH160 PUSHDATA(20)[33e81a941e64cda12c6a299ed322ddbdd03f8d0e] EQUALVERIFY CHECKSIG", script.toString())

        val address = Address(script.getPubKeyHash(), network)
        assertEquals("mkFQohBpy2HDXrCwyMrYL5RtfrmeiuuPY2", address.toString());
    }
}
