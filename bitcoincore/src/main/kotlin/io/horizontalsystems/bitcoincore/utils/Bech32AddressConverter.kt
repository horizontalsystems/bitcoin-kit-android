package io.horizontalsystems.bitcoincore.utils

import io.horizontalsystems.bitcoincore.crypto.Bech32.Encoding
import io.horizontalsystems.bitcoincore.crypto.Bech32Cash
import io.horizontalsystems.bitcoincore.crypto.Bech32Segwit
import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException
import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.bitcoincore.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

abstract class Bech32AddressConverter(var hrp: String) : IAddressConverter

class SegwitAddressConverter(addressSegwitHrp: String) : Bech32AddressConverter(addressSegwitHrp) {
    override fun convert(addressString: String): Address {
        val decoded = Bech32Segwit.decode(addressString)
        if (decoded.hrp != hrp) {
            throw AddressFormatException("Address HRP ${decoded.hrp} is not correct")
        }
        val data = decoded.data
        val stringValue = Bech32Segwit.encode(hrp, decoded.encoding, data)
        val program = Bech32Segwit.convertBits(data, 1, data.size - 1, 5, 8, false)

        return when (val version = data[0].toInt()) {
            0 -> {
                val type = when (program.size) {
                    20 -> AddressType.PubKeyHash
                    32 -> AddressType.ScriptHash
                    else -> throw AddressFormatException("Unknown address type")
                }
                SegWitV0Address(stringValue, program, type)
            }
            1 -> {
                TaprootAddress(stringValue, program, version)
            }
            else -> throw AddressFormatException("Unknown address type")
        }
    }

    override fun convert(lockingScriptPayload: ByteArray, scriptType: ScriptType): Address {
        val witnessScript = Bech32Segwit.convertBits(lockingScriptPayload, 0, lockingScriptPayload.size, 8, 5, true)

        return when (scriptType) {
            ScriptType.P2WPKH -> {
                val addressString = Bech32Segwit.encode(hrp, Encoding.BECH32, byteArrayOf(0.toByte()) + witnessScript)
                SegWitV0Address(addressString, lockingScriptPayload, AddressType.PubKeyHash)
            }
            ScriptType.P2WSH -> {
                val addressString = Bech32Segwit.encode(hrp, Encoding.BECH32, byteArrayOf(0.toByte()) + witnessScript)
                SegWitV0Address(addressString, lockingScriptPayload, AddressType.ScriptHash)
            }
            ScriptType.P2TR -> {
                val addressString = Bech32Segwit.encode(hrp, Encoding.BECH32M, byteArrayOf(1.toByte()) + witnessScript)
                TaprootAddress(addressString, lockingScriptPayload, 1)
            }
            else -> throw AddressFormatException("Unknown Address Type")
        }
    }

    override fun convert(publicKey: PublicKey, scriptType: ScriptType) = when (scriptType) {
        ScriptType.P2WPKH, ScriptType.P2WSH -> {
            convert(OpCodes.scriptWPKH(publicKey.publicKeyHash, versionByte = 0), scriptType)
        }
        ScriptType.P2TR -> {
            convert(OpCodes.scriptWPKH(publicKey.publicKey, versionByte = 1), scriptType)
        }
        else -> throw AddressFormatException("Unknown Address Type")
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
            0 -> AddressType.PubKeyHash
            1 -> AddressType.ScriptHash
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

        return CashAddress(correctedAddress, data.copyOfRange(1, data.size), version, addressType)
    }

    override fun convert(lockingScriptPayload: ByteArray, scriptType: ScriptType): CashAddress {
        val addressType = when (scriptType) {
            ScriptType.P2PKH,
            ScriptType.P2PK -> AddressType.PubKeyHash
            ScriptType.P2SH -> AddressType.ScriptHash
            else -> throw AddressFormatException("Unknown Address Type")
        }

        val payloadSize = lockingScriptPayload.size
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
        val data = byteArrayOf(version.toByte()) + lockingScriptPayload

        // Reserve the number of bytes required for a 5-bit packed version of a
        // hash, with version byte.  Add half a byte(4) so integer math provides
        // the next multiple-of-5 that would fit all the data.

        val addressBytes = ByteArray(((payloadSize + 1) * 8 + 4) / 5)
        Bech32Cash.convertBits(addressBytes, data, 8, 5, true)

        val addressString = Bech32Cash.encode(hrp, addressBytes)

        return CashAddress(addressString, lockingScriptPayload, version, addressType)
    }

    override fun convert(publicKey: PublicKey, scriptType: ScriptType): Address {
        return convert(publicKey.publicKeyHash, scriptType)
    }
}
