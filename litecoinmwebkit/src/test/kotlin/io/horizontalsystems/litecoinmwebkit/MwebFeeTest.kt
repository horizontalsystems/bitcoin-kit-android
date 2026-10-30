package io.horizontalsystems.litecoinmwebkit

import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import org.junit.Assert.assertEquals
import org.junit.Test

/** Expected fees from ltcd's mweb.EstimateFee (go/cmd/vectors). */
class MwebFeeTest {

    private val mweb = "02af20e9da1da85234d313770b40c7ffff99f5a88a8c9c49431ab65520a34fab5b02126e4327cd969b9d510187848ac13081b397e2f615827629a436366ae27d80ab".hexToByteArray()
    private val p2wpkh = "00140000000000000000000000000000000000000000".hexToByteArray()
    private val p2pkh = "76a914000000000000000000000000000000000000000088ac".hexToByteArray()

    @Test
    fun mwebOnly() {
        assertEquals(2100, MwebFee.estimate(listOf(TemplateOutput(mweb, 1000)), 10_000))
        assertEquals(3900, MwebFee.estimate(listOf(TemplateOutput(mweb, 1000), TemplateOutput(mweb, 2000)), 10_000))
    }

    @Test
    fun pegOut() {
        assertEquals(2510, MwebFee.estimate(listOf(TemplateOutput(p2wpkh, 5000), TemplateOutput(mweb, 1000)), 10_000))
        assertEquals(478, MwebFee.estimate(listOf(TemplateOutput(p2wpkh, 5000)), 2_500))
        assertEquals(434, MwebFee.estimate(listOf(TemplateOutput(p2pkh, 5000)), 1_000))
    }
}
