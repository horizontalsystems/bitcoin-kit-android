package bitcoin.wallet.kit.scripts

import bitcoin.wallet.kit.core.hexStringToByteArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class ScriptParserTest {

    @Test
    fun parseChunks() {
        val sigScript = "47304402202b4da291cc39faf8433911988f9f49fc5c995812ca2f94db61468839c228c3e90220628bff3ff32ec95825092fa051cba28558a981fcf59ce184b14f2e215e69106701410414b38f4be3bb9fa0f4f32b74af07152b2f2f630bc02122a491137b6c523e46f18a0d5034418966f93dfc37cc3739ef7b2007213a302b7fba161557f4ad644a1c"
        val chunks = ScriptParser.parseChunks(sigScript.hexStringToByteArray())

        assertEquals(2, chunks.size)
        assertEquals("PUSHDATA(71)[304402202b4da291cc39faf8433911988f9f49fc5c995812ca2f94db61468839c228c3e90220628bff3ff32ec95825092fa051cba28558a981fcf59ce184b14f2e215e69106701]", chunks[0].toString())
        assertEquals("PUSHDATA(65)[0414b38f4be3bb9fa0f4f32b74af07152b2f2f630bc02122a491137b6c523e46f18a0d5034418966f93dfc37cc3739ef7b2007213a302b7fba161557f4ad644a1c]", chunks[1].toString())
    }

    @Test // HASH160 PUSHDATA(20)[4b60b6bcf50bf637fe66c3da5c11524cb3ab9711] EQUAL
    fun isPayToScriptHash() {
        val script = Script("a9144b60b6bcf50bf637fe66c3da5c11524cb3ab971187".hexStringToByteArray())

        assertTrue(ScriptParser.isP2SH(script))
        assertEquals(ScriptType.P2SH, script.getScriptType())
    }

    @Test // PUSHDATA(33)[0378e9dc79ff921df6c3f94d440fb011c65ed03586b1dd01c317934a4f00f251e6] CHECKSIG
    fun isPayToPubKey() {
        val script = Script("210378e9dc79ff921df6c3f94d440fb011c65ed03586b1dd01c317934a4f00f251e6ac".hexStringToByteArray())

        assertTrue(ScriptParser.isP2PK(script))
        assertEquals(ScriptType.P2PK, script.getScriptType())
    }

    @Test // DUP HASH160 PUSHDATA(20)[c860377f38f677fbc6b6061468e4494bb8240255] EQUALVERIFY CHECKSIG
    fun isPayToPubKeyHash() {
        val script = Script("76a914c860377f38f677fbc6b6061468e4494bb824025588ac".hexStringToByteArray())

        assertTrue(ScriptParser.isP2PKH(script))
        assertEquals(ScriptType.P2PKH, script.getScriptType())
    }

}
