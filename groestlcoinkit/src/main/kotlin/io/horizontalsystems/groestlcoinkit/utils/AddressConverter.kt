package io.horizontalsystems.groestlcoinkit.utils

import io.horizontalsystems.groestlcoinkit.crypto.GroestlcoinBase58
import io.horizontalsystems.bitcoincore.crypto.Base58
import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException
import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.AddressType
import io.horizontalsystems.bitcoincore.models.LegacyAddress
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.IAddressConverter
import io.horizontalsystems.groestlcoinkit.GroestlHasher
import java.util.*

class GroestlcoinBase58AddressConverter(private val addressVersion: Int, private val addressScriptVersion: Int) : IAddressConverter {

    override fun convert(addressString: String): Address {
        val data = GroestlcoinBase58.decodeChecked(addressString)
        if (data.size != 20 + 1) {
            throw AddressFormatException("Address length is not 20 hash")
        }

        val bytes = Arrays.copyOfRange(data, 1, data.size)
        val addressType = when (data[0].toInt() and 0xFF) {
            addressScriptVersion -> AddressType.P2SH
            addressVersion -> AddressType.P2PKH
            else -> throw AddressFormatException("Wrong address prefix")
        }

        return LegacyAddress(addressString, bytes, addressType)

    }

    override fun convert(bytes: ByteArray, scriptType: Int): Address {
        val addressType: AddressType
        val addressVersion: Int

        when (scriptType) {
            ScriptType.P2PK,
            ScriptType.P2PKH -> {
                addressType = AddressType.P2PKH
                addressVersion = this.addressVersion
            }
            ScriptType.P2SH,
            ScriptType.P2WPKHSH -> {
                addressType = AddressType.P2SH
                addressVersion = addressScriptVersion
            }

            else -> throw AddressFormatException("Unknown Address Type")
        }

        val addressBytes = byteArrayOf(addressVersion.toByte()) + bytes
        val doubleSHA256 = GroestlHasher().hash(addressBytes)
        val addrChecksum = Arrays.copyOfRange(doubleSHA256, 0, 4)

        val addressString = Base58.encode(addressBytes + addrChecksum)

        return LegacyAddress(addressString, bytes, addressType)
    }

    override fun convert(publicKey: PublicKey, scriptType: Int): Address {
        var keyhash = publicKey.publicKeyHash

        if (scriptType == ScriptType.P2WPKHSH) {
            keyhash = publicKey.scriptHashP2WPKH
        }

        return convert(keyhash, scriptType)
    }
}
