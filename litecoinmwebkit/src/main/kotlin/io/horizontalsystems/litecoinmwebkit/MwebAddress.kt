package io.horizontalsystems.litecoinmwebkit

import io.horizontalsystems.litecoinkit.LitecoinKit

/**
 * An MWEB stealth address: the scan and spend public keys of one address
 * index of an account. The bech32 string form (`ltcmweb1...` on mainnet,
 * `tmweb1...` on testnet) encodes a version byte 0 followed by the 66 bytes
 * of both compressed keys. The same 66 bytes serve as the output script of
 * an MWEB output in a transaction template handed to the daemon.
 */
class MwebAddress(val scanPubKey: ByteArray, val spendPubKey: ByteArray, val hrp: String) {

    init {
        require(isCompressedPubKey(scanPubKey)) { "scan key must be a 33-byte compressed public key" }
        require(isCompressedPubKey(spendPubKey)) { "spend key must be a 33-byte compressed public key" }
    }

    /** Output script for a template transaction output paying this address. */
    val scriptPubKey: ByteArray get() = scanPubKey + spendPubKey

    val stringValue: String by lazy {
        MwebBech32.encode(hrp, byteArrayOf(0) + MwebBech32.convertBits(scriptPubKey, 8, 5, true))
    }

    override fun toString() = stringValue

    override fun equals(other: Any?) = other is MwebAddress && other.stringValue == stringValue

    override fun hashCode() = stringValue.hashCode()

    companion object {
        const val MAINNET_HRP = "ltcmweb"
        const val TESTNET_HRP = "tmweb"
        const val SCRIPT_SIZE = 66

        fun hrp(networkType: LitecoinKit.NetworkType) = when (networkType) {
            LitecoinKit.NetworkType.MainNet -> MAINNET_HRP
            LitecoinKit.NetworkType.TestNet -> TESTNET_HRP
        }

        /** True for the 66-byte scan+spend key script of an MWEB output. */
        fun isMwebScript(script: ByteArray) =
            script.size == SCRIPT_SIZE &&
                    isCompressedPubKey(script.copyOfRange(0, 33)) &&
                    isCompressedPubKey(script.copyOfRange(33, 66))

        fun fromScript(script: ByteArray, hrp: String): MwebAddress {
            if (!isMwebScript(script)) throw MwebAddressFormatException("Not an MWEB output script")
            return MwebAddress(script.copyOfRange(0, 33), script.copyOfRange(33, 66), hrp)
        }

        /** Decodes [address], requiring [expectedHrp] when given. */
        @Throws(MwebAddressFormatException::class)
        fun decode(address: String, expectedHrp: String? = null): MwebAddress {
            val (hrp, data) = MwebBech32.decode(address)
            if (expectedHrp != null && hrp != expectedHrp) {
                throw MwebAddressFormatException("Address prefix $hrp is not $expectedHrp")
            }
            if (hrp != MAINNET_HRP && hrp != TESTNET_HRP) {
                throw MwebAddressFormatException("Unknown MWEB address prefix $hrp")
            }
            if (data.isEmpty() || data[0].toInt() != 0) {
                throw MwebAddressFormatException("Unsupported MWEB address version")
            }
            val script = MwebBech32.convertBits(data.copyOfRange(1, data.size), 5, 8, false)
            if (!isMwebScript(script)) {
                throw MwebAddressFormatException("Invalid MWEB address payload")
            }
            return MwebAddress(script.copyOfRange(0, 33), script.copyOfRange(33, 66), hrp)
        }

        fun isValid(address: String, hrp: String): Boolean = try {
            decode(address, hrp)
            true
        } catch (e: MwebAddressFormatException) {
            false
        }

        private fun isCompressedPubKey(key: ByteArray) =
            key.size == 33 && (key[0].toInt() == 2 || key[0].toInt() == 3)
    }
}

class MwebAddressFormatException(message: String) : Exception(message)

/**
 * Bech32 (BIP173 checksum) without the 90-character length limit: MWEB
 * addresses are 117 characters.
 */
object MwebBech32 {
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private val GENERATOR = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)

    fun encode(hrp: String, data: ByteArray): String {
        val checksum = createChecksum(hrp, data)
        val sb = StringBuilder(hrp.length + 1 + data.size + 6)
        sb.append(hrp).append('1')
        for (b in data) sb.append(CHARSET[b.toInt()])
        for (b in checksum) sb.append(CHARSET[b.toInt()])
        return sb.toString()
    }

    /** Returns the lower-cased human readable part and the 5-bit data without checksum. */
    @Throws(MwebAddressFormatException::class)
    fun decode(str: String): Pair<String, ByteArray> {
        if (str.length < 8) throw MwebAddressFormatException("Address too short")
        var lower = false
        var upper = false
        for (c in str) {
            if (c.code < 33 || c.code > 126) throw MwebAddressFormatException("Invalid character")
            if (c in 'a'..'z') lower = true
            if (c in 'A'..'Z') upper = true
        }
        if (lower && upper) throw MwebAddressFormatException("Mixed case")
        val s = str.lowercase()
        val pos = s.lastIndexOf('1')
        if (pos < 1 || pos + 7 > s.length) throw MwebAddressFormatException("Invalid separator position")
        val hrp = s.substring(0, pos)
        val data = ByteArray(s.length - pos - 1)
        for (i in data.indices) {
            val v = CHARSET.indexOf(s[pos + 1 + i])
            if (v < 0) throw MwebAddressFormatException("Invalid character")
            data[i] = v.toByte()
        }
        if (polymod(hrpExpand(hrp) + data) != 1) throw MwebAddressFormatException("Invalid checksum")
        return hrp to data.copyOfRange(0, data.size - 6)
    }

    fun convertBits(data: ByteArray, fromBits: Int, toBits: Int, pad: Boolean): ByteArray {
        var acc = 0
        var bits = 0
        val maxv = (1 shl toBits) - 1
        val out = ArrayList<Byte>()
        for (b in data) {
            val value = b.toInt() and 0xff
            if (value ushr fromBits != 0) throw MwebAddressFormatException("Invalid data range")
            acc = (acc shl fromBits) or value
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                out.add(((acc ushr bits) and maxv).toByte())
            }
        }
        if (pad) {
            if (bits > 0) out.add(((acc shl (toBits - bits)) and maxv).toByte())
        } else if (bits >= fromBits || ((acc shl (toBits - bits)) and maxv) != 0) {
            throw MwebAddressFormatException("Invalid padding")
        }
        return out.toByteArray()
    }

    private fun polymod(values: ByteArray): Int {
        var chk = 1
        for (v in values) {
            val top = chk ushr 25
            chk = ((chk and 0x1ffffff) shl 5) xor (v.toInt() and 0xff)
            for (i in 0 until 5) {
                if ((top ushr i) and 1 == 1) chk = chk xor GENERATOR[i]
            }
        }
        return chk
    }

    private fun hrpExpand(hrp: String): ByteArray {
        val out = ByteArray(hrp.length * 2 + 1)
        for (i in hrp.indices) {
            out[i] = (hrp[i].code ushr 5).toByte()
            out[i + hrp.length + 1] = (hrp[i].code and 31).toByte()
        }
        return out
    }

    private fun createChecksum(hrp: String, data: ByteArray): ByteArray {
        val values = hrpExpand(hrp) + data + ByteArray(6)
        val mod = polymod(values) xor 1
        return ByteArray(6) { i -> ((mod ushr (5 * (5 - i))) and 31).toByte() }
    }
}
