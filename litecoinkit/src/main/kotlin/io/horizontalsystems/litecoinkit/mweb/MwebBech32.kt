package io.horizontalsystems.litecoinkit.mweb

import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException

/**
 * Bech32m encoder/decoder for MWEB stealth addresses.
 *
 * MWEB addresses encode (scan_pubkey || spend_pubkey) = 66 bytes, which produces
 * ~122-character addresses — well above the standard 90-char Bech32 limit.
 * This implementation omits that restriction as MWEB intentionally exceeds it.
 */
object MwebBech32 {

    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private const val BECH32M_CONST = 0x2bc830a3

    private val CHARSET_REV: ByteArray = ByteArray(128) { -1 }.also { rev ->
        CHARSET.forEachIndexed { i, c -> rev[c.code] = i.toByte() }
    }

    private fun polymod(values: ByteArray): Int {
        var c = 1
        for (vI in values) {
            val c0 = (c ushr 25) and 0xff
            c = ((c and 0x1ffffff) shl 5) xor (vI.toInt() and 0xff)
            if (c0 and 1 != 0) c = c xor 0x3b6a57b2
            if (c0 and 2 != 0) c = c xor 0x26508e6d
            if (c0 and 4 != 0) c = c xor 0x1ea119fa
            if (c0 and 8 != 0) c = c xor 0x3d4233dd
            if (c0 and 16 != 0) c = c xor 0x2a1462b3
        }
        return c
    }

    private fun expandHrp(hrp: String): ByteArray {
        val ret = ByteArray(hrp.length * 2 + 1)
        for (i in hrp.indices) {
            val c = hrp[i].code and 0x7f
            ret[i] = ((c ushr 5) and 0x07).toByte()
            ret[i + hrp.length + 1] = (c and 0x1f).toByte()
        }
        ret[hrp.length] = 0
        return ret
    }

    private fun verifyChecksum(hrp: String, data: ByteArray): Boolean {
        val combined = expandHrp(hrp) + data
        return polymod(combined) == BECH32M_CONST
    }

    private fun createChecksum(hrp: String, data: ByteArray): ByteArray {
        val hrpExpanded = expandHrp(hrp)
        val enc = ByteArray(hrpExpanded.size + data.size + 6)
        hrpExpanded.copyInto(enc, 0)
        data.copyInto(enc, hrpExpanded.size)
        val mod = polymod(enc) xor BECH32M_CONST
        return ByteArray(6) { i -> ((mod ushr (5 * (5 - i))) and 31).toByte() }
    }

    private fun convertBits(data: ByteArray, fromBits: Int, toBits: Int, pad: Boolean): ByteArray {
        var acc = 0
        var bits = 0
        val maxv = (1 shl toBits) - 1
        val result = mutableListOf<Byte>()
        for (b in data) {
            acc = (acc shl fromBits) or (b.toInt() and 0xff)
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                result.add(((acc ushr bits) and maxv).toByte())
            }
        }
        if (pad && bits > 0) {
            result.add(((acc shl (toBits - bits)) and maxv).toByte())
        } else if (!pad && (bits >= fromBits || ((acc shl (toBits - bits)) and maxv) != 0)) {
            if (bits >= fromBits) throw AddressFormatException("Excess padding in MWEB address")
        }
        return result.toByteArray()
    }

    /**
     * Encodes an MWEB stealth address from (scanPubKey, spendPubKey) into a Bech32m string.
     * The result is ~122 characters for mainnet (ltcmweb) and ~120 for testnet (tmweb).
     */
    fun encode(hrp: String, scanPubKey: ByteArray, spendPubKey: ByteArray): String {
        require(scanPubKey.size == 33) { "scanPubKey must be 33 bytes" }
        require(spendPubKey.size == 33) { "spendPubKey must be 33 bytes" }
        val payload = scanPubKey + spendPubKey
        val converted = convertBits(payload, 8, 5, true)
        val data = byteArrayOf(0) + converted  // version 0
        val checksum = createChecksum(hrp, data)
        val combined = data + checksum
        return hrp + "1" + combined.joinToString("") { CHARSET[it.toInt() and 0x1f].toString() }
    }

    /**
     * Decodes a Bech32m MWEB address into (scanPubKey, spendPubKey).
     * Throws [AddressFormatException] on any parse or checksum error.
     */
    fun decode(hrp: String, address: String): Pair<ByteArray, ByteArray> {
        val lower = address.lowercase()
        if (address.any { it.isUpperCase() } && address.any { it.isLowerCase() }) {
            throw AddressFormatException("MWEB address has mixed case")
        }
        val pos = lower.lastIndexOf('1')
        if (pos < 1) throw AddressFormatException("Missing separator in MWEB address")

        val foundHrp = lower.substring(0, pos)
        if (foundHrp != hrp) {
            throw AddressFormatException("Wrong HRP: expected $hrp, got $foundHrp")
        }

        val dataStr = lower.substring(pos + 1)
        if (dataStr.length < 7) throw AddressFormatException("MWEB address data part too short")

        val values = ByteArray(dataStr.length) { i ->
            val c = dataStr[i]
            if (c.code >= CHARSET_REV.size || CHARSET_REV[c.code] < 0) {
                throw AddressFormatException("Invalid character in MWEB address: $c")
            }
            CHARSET_REV[c.code]
        }

        if (!verifyChecksum(hrp, values)) throw AddressFormatException("Invalid MWEB address checksum")

        val data = values.copyOfRange(0, values.size - 6)
        if (data.isEmpty() || data[0].toInt() != 0) {
            throw AddressFormatException("Unsupported MWEB address version: ${data.firstOrNull()}")
        }

        val payload = convertBits(data.copyOfRange(1, data.size), 5, 8, false)
        if (payload.size != 66) {
            throw AddressFormatException("Invalid MWEB payload length: expected 66, got ${payload.size}")
        }

        return payload.copyOfRange(0, 33) to payload.copyOfRange(33, 66)
    }
}