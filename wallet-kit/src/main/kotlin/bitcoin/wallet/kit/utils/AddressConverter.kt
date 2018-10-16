package bitcoin.wallet.kit.utils

import bitcoin.wallet.kit.hdwallet.Address
import bitcoin.wallet.kit.hdwallet.AddressType
import bitcoin.wallet.kit.hdwallet.LegacyAddress
import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.wallet.kit.scripts.ScriptType
import bitcoin.walllet.kit.crypto.Base58
import bitcoin.walllet.kit.exceptions.AddressFormatException
import bitcoin.walllet.kit.utils.Utils
import java.util.*

class AddressConverter(private val network: NetworkParameters) {
    private val bech32 = SegwitAddressConverter()

    fun convert(addressString: String): Address {
        return try {
            bech32.convert(network.addressSegwitHrp, addressString)
        } catch (e: Exception) {
            val data = Base58.decodeChecked(addressString)
            if (data.size != 20 + 1) {
                throw AddressFormatException("Address length is not 20 hash")
            }

            val bytes = Arrays.copyOfRange(data, 1, data.size)
            var type = AddressType.P2PKH
            if (data[0].toInt() == network.addressScriptVersion) {
                type = AddressType.P2SH
            }

            LegacyAddress(addressString, bytes, type)
        }
    }

    fun convert(bytes: ByteArray, type: Int = ScriptType.P2PKH): Address {

        if (type == ScriptType.P2WSH || type == ScriptType.P2WPKH) try {
            return bech32.convert(network.addressSegwitHrp, bytes)
        } catch (e: Exception) {
            // ignore and try to convert to legacy address
        }

        val addressType: AddressType
        val addressVersion: Int

        when (type) {
            ScriptType.P2PK,
            ScriptType.P2PKH -> {
                addressType = AddressType.P2PKH
                addressVersion = network.addressVersion
            }
            ScriptType.P2SH -> {
                addressType = AddressType.P2SH
                addressVersion = network.addressScriptVersion
            }

            else -> throw AddressConverterException.UnknownAddressType()
        }

        val addressBytes = byteArrayOf(addressVersion.toByte()) + bytes
        val doubleSHA256 = Utils.doubleDigest(addressBytes)
        val addrChecksum = Arrays.copyOfRange(doubleSHA256, 0, 4)

        val addressString = Base58.encode(addressBytes + addrChecksum)

        return LegacyAddress(addressString, bytes, addressType)
    }
}

open class AddressConverterException(message: String) : Exception(message) {
    class UnknownAddressType : AddressConverterException("Unknown Address Type")
}

