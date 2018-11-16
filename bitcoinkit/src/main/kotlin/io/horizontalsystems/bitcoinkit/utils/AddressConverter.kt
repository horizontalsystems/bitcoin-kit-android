package io.horizontalsystems.bitcoinkit.utils

import io.horizontalsystems.bitcoinkit.crypto.Base58
import io.horizontalsystems.bitcoinkit.exceptions.AddressFormatException
import io.horizontalsystems.bitcoinkit.models.Address
import io.horizontalsystems.bitcoinkit.models.AddressType
import io.horizontalsystems.bitcoinkit.models.LegacyAddress
import io.horizontalsystems.bitcoinkit.network.MainNetBitcoinCash
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.bitcoinkit.network.TestNetBitcoinCash
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType
import java.util.*

class AddressConverter(private val network: Network) {
    private val bech32 = when (network) {
        is MainNetBitcoinCash,
        is TestNetBitcoinCash -> CashAddressConverter()
        // MainNet, TestNet, RegTest
        else -> SegwitAddressConverter()
    }

    @Throws
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

    @Throws
    fun convert(bytes: ByteArray, scriptType: Int = ScriptType.P2PKH): Address {

        try {
            return bech32.convert(network.addressSegwitHrp, bytes, scriptType)
        } catch (e: AddressFormatException) {
            // ignore and try to convert to legacy address
        }

        val addressType: AddressType
        val addressVersion: Int

        when (scriptType) {
            ScriptType.P2PK,
            ScriptType.P2PKH -> {
                addressType = AddressType.P2PKH
                addressVersion = network.addressVersion
            }
            ScriptType.P2SH -> {
                addressType = AddressType.P2SH
                addressVersion = network.addressScriptVersion
            }

            else -> throw AddressFormatException("Unknown Address Type")
        }

        val addressBytes = byteArrayOf(addressVersion.toByte()) + bytes
        val doubleSHA256 = Utils.doubleDigest(addressBytes)
        val addrChecksum = Arrays.copyOfRange(doubleSHA256, 0, 4)

        val addressString = Base58.encode(addressBytes + addrChecksum)

        return LegacyAddress(addressString, bytes, addressType)
    }
}
