package io.horizontalsystems.bitcoincore.utils

import io.horizontalsystems.bitcoincore.crypto.Bech32Cash
import io.horizontalsystems.bitcoincore.crypto.Bech32Segwit
import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException
import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.bitcoincore.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoincore.transactions.scripts.Script
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import java.util.*

abstract class Bech32AddressConverter(var hrp: String) : IAddressConverter

class SegwitAddressConverter(addressSegwitHrp: String) : Bech32AddressConverter(addressSegwitHrp) {
    override fun convert(addressString: String): SegWitAddress {
        val decoded = Bech32Segwit.decode(addressString)
        if (decoded.hrp != hrp) {
            throw AddressFormatException("Address HRP ${decoded.hrp} is not correct")
        }

        val payload = decoded.data
        val string = Bech32Segwit.encode(hrp, payload)
        val program = Bech32Segwit.convertBits(payload, 1, payload.size - 1, 5, 8, false)

        return SegWitAddress(string, program, AddressType.WITNESS, 0)
    }

    override fun convert(bytes: ByteArray, scriptType: ScriptType): SegWitAddress {
        val addressType = when (scriptType) {
            ScriptType.P2WPKH -> AddressType.WITNESS
            ScriptType.P2WSH -> AddressType.WITNESS
            else -> throw AddressFormatException("Unknown Address Type")
        }

        val script = Script(bytes)
        val version = witnessVersion(script.chunks[0].opcode)
        val keyHash = script.chunks[1].data
        if (keyHash == null || version == null) {
            throw AddressFormatException("Invalid address size")
        }

        val witnessScript = Bech32Segwit.convertBits(keyHash, 0, keyHash.size, 8, 5, true)
        val addressString = Bech32Segwit.encode(hrp, byteArrayOf(version.toByte()) + witnessScript)

        return SegWitAddress(addressString, keyHash, addressType, version)
    }

    override fun convert(publicKey: PublicKey, scriptType: ScriptType): Address {
        val keyHash = OpCodes.scriptWPKH(publicKey.publicKeyHash)
        return convert(keyHash, scriptType)
    }

    private fun witnessVersion(opcode: Int): Int? {
        //  OP_0 is encoded as 0x00
        if (opcode == 0) {
            return opcode
        }

        //  OP_1 through OP_16 are encoded as 0x51 though 0x60
        val version = opcode - 0x50
        if (version in 1..16) {
            return version
        }

        return null
    }
}

class CashAddressConverter(addressSegwitHrp: String) : Bech32AddressConverter(addressSegwitHrp) {
    override fun convert(addressString: String): CashAddress {
        var correctedAddress = addressString
        if (addressString.indexOf(":") < 0) {
            correctedAddress = "$hrp:$addressString"
        }

        val decoded = Bech32Cash.decode(correctedAddress, hrp)
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
        Bech32Cash.convertBits(data, payload, 5, 8, false)

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

        return CashAddress(correctedAddress, data.copyOfRange(1, data.size), addressType)
    }

    override fun convert(bytes: ByteArray, scriptType: ScriptType): CashAddress {
        val addressType = when (scriptType) {
            ScriptType.P2PKH,
            ScriptType.P2PK -> AddressType.P2PKH
            ScriptType.P2SH -> AddressType.P2SH
            else -> throw AddressFormatException("Unknown Address Type")
        }

        val payloadSize = bytes.size
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
        val data = byteArrayOf(version.toByte()) + bytes

        // Reserve the number of bytes required for a 5-bit packed version of a
        // hash, with version byte.  Add half a byte(4) so integer math provides
        // the next multiple-of-5 that would fit all the data.

        val addressBytes = ByteArray(((payloadSize + 1) * 8 + 4) / 5)
        Bech32Cash.convertBits(addressBytes, data, 8, 5, true)

        val addressString = Bech32Cash.encode(hrp, addressBytes)

        return CashAddress(addressString, bytes, addressType)
    }

    override fun convert(publicKey: PublicKey, scriptType: ScriptType): Address {
        return convert(publicKey.publicKeyHash, scriptType)
    }
}
