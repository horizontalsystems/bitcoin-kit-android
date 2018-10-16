package bitcoin.wallet.kit.utils

import bitcoin.wallet.kit.hdwallet.Address
import bitcoin.wallet.kit.hdwallet.AddressType
import bitcoin.wallet.kit.hdwallet.CashAddress
import bitcoin.wallet.kit.hdwallet.SegWitAddress
import bitcoin.wallet.kit.scripts.ScriptType
import bitcoin.walllet.kit.crypto.Bech32
import bitcoin.walllet.kit.crypto.CashBech32
import bitcoin.walllet.kit.exceptions.AddressFormatException
import java.util.*

abstract class Bech32AddressConverter {
    abstract fun convert(hrp: String, addressString: String): Address
    abstract fun convert(hrp: String, program: ByteArray, scriptType: Int): Address
}

class SegwitAddressConverter : Bech32AddressConverter() {
    override fun convert(hrp: String, addressString: String): SegWitAddress {
        val decoded = Bech32.decode(addressString)
        if (decoded.hrp != hrp) {
            throw AddressFormatException("Address HRP ${decoded.hrp} is not correct")
        }

        val payload = decoded.data
        val string = Bech32.encode(hrp, payload)
        val program = Bech32.convertBits(payload, 1, payload.size - 1, 5, 8, false)

        return SegWitAddress(string, program, AddressType.WITNESS)
    }

    override fun convert(hrp: String, program: ByteArray, scriptType: Int): SegWitAddress {
        val addressType = when (scriptType) {
            ScriptType.P2WPKH -> AddressType.WITNESS
            ScriptType.P2WSH -> AddressType.WITNESS
            else -> throw AddressFormatException("Unknown Address Type")
        }

        val version = byteArrayOf(0)
        val bytes = version + Bech32.convertBits(program, 0, program.size, 8, 5, true)
        val string = Bech32.encode(hrp, bytes)

        return SegWitAddress(string, program, addressType)
    }
}

class CashAddressConverter : Bech32AddressConverter() {
    override fun convert(hrp: String, addressString: String): CashAddress {

        val decoded = CashBech32.decode(addressString, hrp)
        if (decoded.hrp != hrp) {
            throw AddressFormatException("Invalid prefix for network: ${decoded.hrp} != $hrp (expected)")
        }

        val payload = decoded.data
        if (payload.isEmpty()) {
            throw AddressFormatException("No payload")
        }

        // Check that the padding is zero.
        val extraBits = (payload.size * 5 % 8).toByte()
        if (extraBits >= 5) {
            throw AddressFormatException("More than allowed padding")
        }

        val data = ByteArray(payload.size * 5 / 8)
        CashBech32.convertBits(data, payload, 5, 8, false)

        // Decode type and size from the version.
        val version = data[0].toInt()
        if (version and 0x80 != 0) {
            throw AddressFormatException("First bit is reserved")
        }

        val addressType = when ((version shr 3) and 0x1f) {
            0 -> AddressType.P2PKH
            1 -> AddressType.P2SH
            else -> throw AddressFormatException("Unknown Address Type")
        }

        var hashSize = 20 + 4 * (version and 0x03)
        if (version and 0x04 != 0) {
            hashSize *= 2
        }

        // Check that we decoded the exact number of bytes we expected.
        if (data.size != hashSize + 1) {
            throw AddressFormatException("Data length ${data.size} != hash size $hashSize")
        }

        return CashAddress(addressString, Arrays.copyOfRange(data, 1, data.size), addressType)
    }

    override fun convert(hrp: String, program: ByteArray, scriptType: Int): CashAddress {
        val addressType = when (scriptType) {
            ScriptType.P2PKH,
            ScriptType.P2PK -> AddressType.P2PKH
            ScriptType.P2SH -> AddressType.P2SH
            else -> throw AddressFormatException("Unknown Address Type")
        }

        val payloadSize = program.size
        val encodedSize = when (payloadSize * 8) {
            160 -> 0
            192 -> 1
            224 -> 2
            256 -> 3
            320 -> 4
            384 -> 5
            448 -> 6
            512 -> 7
            else -> throw AddressFormatException("Invalid address length")
        }

        val version = (addressType.ordinal shl 3) or encodedSize
        val data = byteArrayOf(version.toByte()) + program

        // Reserve the number of bytes required for a 5-bit packed version of a
        // hash, with version byte.  Add half a byte(4) so integer math provides
        // the next multiple-of-5 that would fit all the data.

        val addressBytes = ByteArray(((payloadSize + 1) * 8 + 4) / 5)
        CashBech32.convertBits(addressBytes, data, 8, 5, true)

        val addressString = CashBech32.encode(hrp, addressBytes)

        return CashAddress(addressString, program, addressType)
    }
}
