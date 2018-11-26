package io.horizontalsystems.bitcoinkit.transactions.scripts

import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptParserTest {
    lateinit var script: Script

    @Test
    fun parseChunks() {
        val sigScript = "47304402202b4da291cc39faf8433911988f9f49fc5c995812ca2f94db61468839c228c3e90220628bff3ff32ec95825092fa051cba28558a981fcf59ce184b14f2e215e69106701410414b38f4be3bb9fa0f4f32b74af07152b2f2f630bc02122a491137b6c523e46f18a0d5034418966f93dfc37cc3739ef7b2007213a302b7fba161557f4ad644a1c"
        val chunks = ScriptParser.parseChunks(sigScript.hexStringToByteArray())

        assertEquals(2, chunks.size)
        assertEquals("PUSHDATA(71)[304402202b4da291cc39faf8433911988f9f49fc5c995812ca2f94db61468839c228c3e90220628bff3ff32ec95825092fa051cba28558a981fcf59ce184b14f2e215e69106701]", chunks[0].toString())
        assertEquals("PUSHDATA(65)[0414b38f4be3bb9fa0f4f32b74af07152b2f2f630bc02122a491137b6c523e46f18a0d5034418966f93dfc37cc3739ef7b2007213a302b7fba161557f4ad644a1c]", chunks[1].toString())
    }

    @Test
    fun parseChunks_input() {
        val sigScript = "483045022050eb59c79435c051f45003d9f82865c8e4df5699d7722e77113ef8cadbd92109022100d4ab233e070070eb8e0e62e3d2d2eb9474a5bf135c9eda32755acb0875a6c20601"
        val chunks = ScriptParser.parseChunks(sigScript.hexStringToByteArray())

        assertEquals(1, chunks.size)
        assertEquals("PUSHDATA(72)[3045022050eb59c79435c051f45003d9f82865c8e4df5699d7722e77113ef8cadbd92109022100d4ab233e070070eb8e0e62e3d2d2eb9474a5bf135c9eda32755acb0875a6c20601]", chunks[0].toString())
    }

    @Test // HASH160 PUSHDATA(20)[4b60b6bcf50bf637fe66c3da5c11524cb3ab9711] EQUAL
    fun isPayToScriptHash() {
        script = Script("a9144b60b6bcf50bf637fe66c3da5c11524cb3ab971187".hexStringToByteArray())

        assertTrue(ScriptParser.isP2SH(script))
        assertEquals(ScriptType.P2SH, script.getScriptType())
    }

    @Test // OP_0 32 0xa99d08fbec6958f4d4a3776c3728ec448934d25fe1142054b8b68bac866dfc3a
    fun isPayToWitnessSHash() {
        script = Script("0020a99d08fbec6958f4d4a3776c3728ec448934d25fe1142054b8b68bac866dfc3a".hexStringToByteArray())

        assertTrue(ScriptParser.isP2WSH(script))
        assertEquals(ScriptType.P2WSH, script.getScriptType())
    }

    @Test // OP_0 20 0x799d283e7f92af1dd242bf4eea513c6efd117de2
    fun isPayToWitnessPubkeyhash() {
        script = Script("0014799d283e7f92af1dd242bf4eea513c6efd117de2".hexStringToByteArray())

        assertTrue(ScriptParser.isP2WPKH(script))
        assertEquals(ScriptType.P2WPKH, script.getScriptType())
    }

    @Test // PUSHDATA(33)[0378e9dc79ff921df6c3f94d440fb011c65ed03586b1dd01c317934a4f00f251e6] CHECKSIG
    fun isPayToPubKey() {
        script = Script("210378e9dc79ff921df6c3f94d440fb011c65ed03586b1dd01c317934a4f00f251e6ac".hexStringToByteArray())

        assertTrue(ScriptParser.isP2PK(script))
        assertEquals(ScriptType.P2PK, script.getScriptType())
    }

    @Test // DUP HASH160 PUSHDATA(20)[c860377f38f677fbc6b6061468e4494bb8240255] EQUALVERIFY CHECKSIG
    fun isPayToPubKeyHash() {
        script = Script("76a914c860377f38f677fbc6b6061468e4494bb824025588ac".hexStringToByteArray())

        assertTrue(ScriptParser.isP2PKH(script))
        assertEquals(ScriptType.P2PKH, script.getScriptType())
    }

    @Test // TestNet: cdf0a864fb349400b2f606262b3e03939b3c0077d509655c5b221ff3367998ed
    fun isPKHashInput() {
        script = Script("4830450221009b1fc7f43826c4c61bb0bc7c9f667c7383f728647563f19f75db6ca701f4326f02205bc4dd125ffe4e903a59c87ed4362abb86acbf2b1e6fd8021cfc6d4f8384b0e5012103b1ae868b76e84f8ae1cb3ad958653d8a23b444e8639c0c0f00e8de27541cb977".hexStringToByteArray())
        assertTrue(ScriptParser.isPKHashInput(script))
    }

    @Test // TestNet: e1c1e14a966e4a2677d06145078c41778926bfbd2da3430ce6f6dcb6ada68f02
    fun isSHashInput() {
        script = Script("220020770718025d04d9b863c7e896f16f8499ffadca9e76c3f11e2164d052054ad9d9".hexStringToByteArray())
        assertTrue(ScriptParser.isSHashInput(script))
    }

    @Test // MainNet: d09fbad860dd6fcce5b85bcd865dcb173497101d43e88768bbef3a1bb09078aa
    fun isMultisigInput() {
        script = Script("0047304402203fc1c9428ed4104c1a592fd6121267152c7eea7d719857dd96e3116af5d2d4f302203cf3ba36fc78cb283b5905f5e0903ac1e05e2f84355c51f06f689b90d73aaeff014730440220754d77ef7d103fb1a68838684719a887d2798583cc89850e7804b27ffd94129c02205a2947224de539eee04f6e105d32c812e044a2dd2bdf6ac80f63491a56904b74014c69522103a4dcd85b4d8040b604a2bad2070c04cf59e64f574f8669f42c97185f4420e0a221030fa581544a5cd3d8374027614cecacd20ed11c43ec3db0a05cdf2719feee77a221035ad856ca3661d4bf44dbedc31dcaf4d1e549fa9c67fe8729b8433dabd8f0e25f53ae".hexStringToByteArray())

        assertTrue(ScriptParser.isMultiSigInput(script))
    }

    @Test // MainNet: 5c85ed63469aa9971b5d01063dbb8bcdafd412b2f51a3d24abf2e310c028bbf8
    fun isPubKeyInput() {
        script = Script("483045022050eb59c79435c051f45003d9f82865c8e4df5699d7722e77113ef8cadbd92109022100d4ab233e070070eb8e0e62e3d2d2eb9474a5bf135c9eda32755acb0875a6c20601".hexStringToByteArray())
        assertTrue(ScriptParser.isPubKeyInput(script))
    }
}
