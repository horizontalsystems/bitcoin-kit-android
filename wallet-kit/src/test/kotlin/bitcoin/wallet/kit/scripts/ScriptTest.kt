package bitcoin.wallet.kit.scripts

import bitcoin.wallet.kit.core.hexStringToByteArray
import bitcoin.wallet.kit.core.toHexString
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

    @Test
    fun getPubKeyHashIn_PKH() {
        val pk = "03b1ae868b76e84f8ae1cb3ad958653d8a23b444e8639c0c0f00e8de27541cb977"
        script = Script("4830450221009b1fc7f43826c4c61bb0bc7c9f667c7383f728647563f19f75db6ca701f4326f02205bc4dd125ffe4e903a59c87ed4362abb86acbf2b1e6fd8021cfc6d4f8384b0e50121$pk".hexStringToByteArray())

        assertEquals(pk, script.chunks[1].data?.toHexString())
        assertEquals("50f9c5f3f6552f23bceb10761612bf3b9ff77a27", script.getPubKeyHashIn()?.toHexString())
    }

    @Test // tx: 761cc7102efe24f4353ae7dc816fbed5e15963d11ca93e36449d521bda21ac4d
    fun getPubKeyHashIn_SH() {
        script = Script("004830450221008c203a0881f75c731d9a3a2e6d2ffa37da7095b7dde61a9e7a906659219cd0fa02202677097ca7f7e164f73924fe8f84e1e6fc6611450efcda360ce771e98af9f73d0147304402201cba9b641483476f67a4cef08d7280f51de8d7615fcce76642d944dc07132a990220323d13175477bbf67c8c36fb243bec0e4c410bc9173a186d9f8e98ce3445363601475221025b64f7c63e30f315259393f64dcca269d18386997b1cc93da1388c4021e3ea8e210386d42d5d7027ac08ddcbb066e2140575091fe7dc1d202a008eb5e036725e975652ae".hexStringToByteArray())

        val keyHash = script.getPubKeyHashIn()
        assertEquals("aed6f804c63da80800892f8fd4cdbad0d3ad6d12", keyHash?.toHexString())
    }
}
