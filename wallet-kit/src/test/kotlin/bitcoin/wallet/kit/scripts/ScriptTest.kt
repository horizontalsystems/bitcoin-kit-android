package bitcoin.wallet.kit.scripts

import bitcoin.wallet.kit.core.hexStringToByteArray
import bitcoin.wallet.kit.network.TestNet
import bitcoin.walllet.kit.hdwallet.Address
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptTest {
    private val sigScript = "47304402202b4da291cc39faf8433911988f9f49fc5c995812ca2f94db61468839c228c3e90220628bff3ff32ec95825092fa051cba28558a981fcf59ce184b14f2e215e69106701410414b38f4be3bb9fa0f4f32b74af07152b2f2f630bc02122a491137b6c523e46f18a0d5034418966f93dfc37cc3739ef7b2007213a302b7fba161557f4ad644a1c"
    private val network = TestNet()

    lateinit var script: Script

    @Test
    fun scriptSig() {
        script = Script(sigScript.hexStringToByteArray())

        assertEquals("PUSHDATA(71)[304402202b4da291cc39faf8433911988f9f49fc5c995812ca2f94db61468839c228c3e90220628bff3ff32ec95825092fa051cba28558a981fcf59ce184b14f2e215e69106701] PUSHDATA(65)[0414b38f4be3bb9fa0f4f32b74af07152b2f2f630bc02122a491137b6c523e46f18a0d5034418966f93dfc37cc3739ef7b2007213a302b7fba161557f4ad644a1c]", script.toString())
    }

    @Test
    fun getPubKeyHash_P2PKH() {
        script = Script("76a91433e81a941e64cda12c6a299ed322ddbdd03f8d0e88ac".hexStringToByteArray())

        assertEquals(ScriptType.P2PKH, script.getScriptType())
        assertEquals("DUP HASH160 PUSHDATA(20)[33e81a941e64cda12c6a299ed322ddbdd03f8d0e] EQUALVERIFY CHECKSIG", script.toString())

        val address = Address(script.getPubKeyHash(), network)
        assertEquals("mkFQohBpy2HDXrCwyMrYL5RtfrmeiuuPY2", address.toString())
    }

    @Test
    fun getPubKeyHash_P2PK() {
        script = Script("210378e9dc79ff921df6c3f94d440fb011c65ed03586b1dd01c317934a4f00f251e6ac".hexStringToByteArray())

        assertEquals(ScriptType.P2PK, script.getScriptType())
        assertEquals("PUSHDATA(33)[0378e9dc79ff921df6c3f94d440fb011c65ed03586b1dd01c317934a4f00f251e6] CHECKSIG", script.toString())

        val address = Address(script.getPubKeyHash(), network)
        assertEquals("mv25Sgz24fMQFQHD5QNmLaRTGDmztV3xsK", address.toString())
    }

    @Test
    fun getPubKeyHash_P2SH() {
        script = Script("a9144b60b6bcf50bf637fe66c3da5c11524cb3ab971187".hexStringToByteArray())

        assertEquals(ScriptType.P2SH, script.getScriptType())
        assertEquals("HASH160 PUSHDATA(20)[4b60b6bcf50bf637fe66c3da5c11524cb3ab9711] EQUAL", script.toString())

        val address = Address(script.getPubKeyHash(), network)
        assertEquals("mnPWqEc45zz18exrfiHFcDPP9dxzjmtvWw", address.toString())
    }

    @Test
    fun isCode() {
        script = Script("220020770718025d04d9b863c7e896f16f8499ffadca9e76c3f11e2164d052054ad9d9".hexStringToByteArray())

        assertTrue(script.isCode())
    }
}
