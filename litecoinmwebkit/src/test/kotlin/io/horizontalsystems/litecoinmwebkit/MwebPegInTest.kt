package io.horizontalsystems.litecoinmwebkit

import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class MwebPegInTest {

    private val ownScript = "021e6fb3b0f0a3efac6f1ec1e2a7cdfc429be04ae8c6ad22899a3585aed3a00a35025bd358721374281095c82ea7c5af2bf16780c5191683785b30730921c3ea47f1".hexToByteArray()
    private val foreignAddress = "ltcmweb1qqdylm4v7qsaxzgapu7my7lxqhje9vec4sec47a89cprxxxzv2q2d6qs6k9egg95t8c6v3u42jjwtsa9levmdm97tg4hk9jyyhfvvs3dsd5yqndj5"
    private val foreignScript = MwebAddress.decode(foreignAddress).scriptPubKey
    private val changeScript = "00140000000000000000000000000000000000000000".hexToByteArray()
    private val prevHash = ByteArray(32) { 0x11 }
    private val value = 1_000_000L
    private val mwebFee = 2100L

    /** What the Litecoin wallet hands over: value + MWEB fee to the MWEB output, then its change. */
    private fun unsigned(withChange: Boolean): ByteArray {
        val outputs = mutableListOf(TemplateOutput(foreignScript, value + mwebFee))
        if (withChange) outputs.add(TemplateOutput(changeScript, 5_000_000L))
        return MwebTxSerializer.serialize(
            RawTransaction(2, mutableListOf(RawInput(prevHash, 1, byteArrayOf(), 0xfffffffeL)), outputs, null, null, 0)
        )
    }

    @Test
    fun templateForForeignAddress() {
        val template = MwebPegIn.template(unsigned(true), 1, foreignScript, value)
        val tx = MwebTxSerializer.parse(template.rawTx)
        assertEquals(1, tx.outputs.size)
        assertArrayEquals(foreignScript, tx.outputs[0].script)
        assertEquals(value, tx.outputs[0].value)
        assertEquals(1, tx.inputs.size)
        assertArrayEquals(prevHash, tx.inputs[0].previousHash)
        assertNotNull(template.change)
        assertArrayEquals(changeScript, template.change!!.script)
        assertEquals(5_000_000L, template.change!!.value)
    }

    @Test
    fun templateWithoutChange() {
        val template = MwebPegIn.template(unsigned(false), null, foreignScript, value)
        assertNull(template.change)
        assertEquals(1, MwebTxSerializer.parse(template.rawTx).outputs.size)
    }

    @Test
    fun rejectsNonMwebDestination() {
        assertThrows(IllegalArgumentException::class.java) { MwebPegIn.template(unsigned(true), 1, changeScript, value) }
    }

    @Test
    fun withChangeChecksPegInOutputAndAppendsChange() {
        val pegInScript = "5920".hexToByteArray() + ByteArray(32) { 0x22 }
        val created = MwebTxSerializer.serialize(
            RawTransaction(
                2, mutableListOf(RawInput(prevHash, 1, byteArrayOf(), 0xfffffffeL)),
                mutableListOf(TemplateOutput(pegInScript, value + mwebFee)), null, "aa".hexToByteArray(), 0
            )
        )
        val change = TemplateOutput(changeScript, 5_000_000L)
        val tx = MwebTxSerializer.parse(MwebPegIn.withChange(created, change, value + mwebFee))
        assertEquals(2, tx.outputs.size)
        assertArrayEquals(pegInScript, tx.outputs[0].script)
        assertArrayEquals(changeScript, tx.outputs[1].script)
        assertArrayEquals("aa".hexToByteArray(), tx.mweb)

        assertThrows(IllegalStateException::class.java) { MwebPegIn.withChange(created, change, value) }
        val noMweb = MwebTxSerializer.serialize(
            RawTransaction(2, mutableListOf(RawInput(prevHash, 1, byteArrayOf(), 0xfffffffeL)), mutableListOf(TemplateOutput(ownScript, value)), null, null, 0)
        )
        assertThrows(IllegalStateException::class.java) { MwebPegIn.withChange(noMweb, change, value + mwebFee) }
    }
}
