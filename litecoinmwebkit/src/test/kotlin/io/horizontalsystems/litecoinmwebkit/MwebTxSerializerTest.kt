package io.horizontalsystems.litecoinmwebkit

import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.extensions.toHexString
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MwebTxSerializerTest {

    private val outputId = "0102030405060708091011121314151617181920212223242526272829303132".hexToByteArray()
    private val mwebScript = "02af20e9da1da85234d313770b40c7ffff99f5a88a8c9c49431ab65520a34fab5b02126e4327cd969b9d510187848ac13081b397e2f615827629a436366ae27d80ab".hexToByteArray()

    @Test
    fun template() {
        val raw = MwebTxSerializer.serializeTemplate(
            listOf(TemplateInput(outputId, 7)),
            listOf(TemplateOutput(mwebScript, 123456)),
        )
        val expected = "02000000" +
                "01" + outputId.toHexString() + "07000000" + "00" + "ffffffff" +
                "01" + "40e2010000000000" + "42" + mwebScript.toHexString() +
                "00000000"
        assertEquals(expected, raw.toHexString())

        val parsed = MwebTxSerializer.parse(raw)
        assertEquals(2, parsed.version)
        assertEquals(1, parsed.inputs.size)
        assertEquals(7L, parsed.inputs[0].previousIndex)
        assertEquals(123456L, parsed.outputs[0].value)
        assertNull(parsed.witnesses)
        assertNull(parsed.mweb)
    }

    @Test
    fun mwebExtensionIsOpaqueAndPreserved() {
        val mwebBlob = "deadbeef0102".hexToByteArray()
        val tx = RawTransaction(
            version = 2,
            inputs = mutableListOf(RawInput(outputId, 0, byteArrayOf(), 0xfffffffeL)),
            outputs = mutableListOf(TemplateOutput("5920".hexToByteArray() + ByteArray(32), 5000)),
            witnesses = mutableListOf(listOf("3044".hexToByteArray(), "02ab".hexToByteArray())),
            mweb = mwebBlob,
            lockTime = 0,
        )
        val raw = MwebTxSerializer.serialize(tx)
        assertEquals("0200000000" + "09", raw.toHexString().substring(0, 12))
        val parsed = MwebTxSerializer.parse(raw)
        assertNotNull(parsed.witnesses)
        assertEquals(2, parsed.witnesses!![0].size)
        assertArrayEquals(mwebBlob, parsed.mweb)
        assertEquals(5000L, parsed.outputs[0].value)
        assertEquals(0L, parsed.lockTime)

        val stripped = MwebTxSerializer.serialize(parsed, includeWitness = true, includeMweb = false)
        assertEquals("0200000000" + "01", stripped.toHexString().substring(0, 12))
        val reparsed = MwebTxSerializer.parse(stripped)
        assertNull(reparsed.mweb)
        assertEquals(1, reparsed.outputs.size)

        val legacy = MwebTxSerializer.serialize(parsed, includeWitness = false, includeMweb = false)
        assertEquals("02000000" + "01", legacy.toHexString().substring(0, 10))
    }

    @Test
    fun varIntSizes() {
        assertEquals(1, MwebTxSerializer.varIntSize(0))
        assertEquals(1, MwebTxSerializer.varIntSize(252))
        assertEquals(3, MwebTxSerializer.varIntSize(253))
        assertEquals(5, MwebTxSerializer.varIntSize(70000))
    }
}
