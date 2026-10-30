package io.horizontalsystems.litecoinmwebkit

import kotlin.math.ceil

/**
 * MWEB fee rules as implemented by the daemon's `mweb.EstimateFee`: a kernel
 * with stealth excess weighs 3, every MWEB output 18, and a peg-out output
 * `ceil(scriptLength / 42)` plus its serialized size charged at the
 * Litecoin fee rate; each unit of weight costs 100 litoshi.
 *
 * When the template contains MWEB inputs whose value exceeds the outputs plus
 * this fee, the daemon makes the surplus part of the fee, so a change output
 * must be included in the template and valued accordingly.
 */
object MwebFee {
    const val BASE_MWEB_FEE = 100L
    const val KERNEL_WITH_STEALTH_WEIGHT = 3L
    const val STANDARD_OUTPUT_WEIGHT = 18L
    const val BYTES_PER_WEIGHT = 42L

    /** Extra virtual size of the HogEx input a peg-in adds to the next block. */
    const val HOGEX_INPUT_VBYTES = 41

    fun estimate(outputs: List<TemplateOutput>, feeRatePerKb: Long): Long {
        var weight = KERNEL_WITH_STEALTH_WEIGHT
        var txOutSize = 0L
        for (output in outputs) {
            if (MwebAddress.isMwebScript(output.script)) {
                weight += STANDARD_OUTPUT_WEIGHT
            } else {
                weight += (output.script.size + BYTES_PER_WEIGHT - 1) / BYTES_PER_WEIGHT
                txOutSize += 8 + MwebTxSerializer.varIntSize(output.script.size.toLong()) + output.script.size
            }
        }
        val ltcFee = ceil(feeRatePerKb.toDouble() * txOutSize / 1000.0).toLong()
        return ltcFee + weight * BASE_MWEB_FEE
    }
}
