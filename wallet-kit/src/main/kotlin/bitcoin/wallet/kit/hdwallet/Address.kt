package bitcoin.wallet.kit.hdwallet

import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.walllet.kit.crypto.Base58
import bitcoin.walllet.kit.crypto.Bech32
import bitcoin.walllet.kit.exceptions.AddressFormatException
import bitcoin.walllet.kit.utils.Utils
import java.util.*


class Address {
    enum class Type {
        P2PKH,  // Pay to public key hash
        P2SH,   // Pay to script hash
        WITNESS // Pay to witness hash
    }

    lateinit var type: Type
    lateinit var hash: ByteArray

    private val network: NetworkParameters

    constructor(type: Type, hash: ByteArray, network: NetworkParameters) {
        this.type = type
        this.hash = hash
        this.network = network
    }

    constructor(address: String, network: NetworkParameters) {
        this.network = network

        if (isMixedCase(address))
            return fromBase58(address)

        try {
            fromBech32(address)
        } catch (e: Exception) {
            fromBase58(address)
        }
    }

    private fun fromBase58(address: String) {
        val data = Base58.decodeChecked(address)
        if (data.size != 20 + 1) {
            throw AddressFormatException("Address length is not 20 bytes")
        }

        type = getType(data[0].toInt() and 0xff)
        hash = Arrays.copyOfRange(data, 1, data.size)
    }

    private fun fromBech32(address: String) {
        val decoded = Bech32.decode(address)
        if (decoded.hrp != network.addressSegwitHrp) {
            throw AddressFormatException("Address HRP ${decoded.hrp} is not correct")
        }

        type = Type.WITNESS
        hash = decoded.data
    }

    private fun getType(version: Int): Type {
        if (version == network.addressHeader) {
            return Type.P2PKH
        } else if (version == network.scriptAddressHeader) {
            return Type.P2SH
        }

        throw AddressFormatException("Address version $version is not correct")
    }

    private fun isMixedCase(address: String): Boolean {
        var lower = false
        var upper = false

        for (char in address) {
            val code = char.hashCode()
            if (code in 0x30..0x39)
                continue

            if (code and 32 > 0) {
                check(code in 0x61..0x7a)
                lower = true
            } else {
                check(code in 0x41..0x5a)
                upper = true
            }

            if (lower && upper)
                return true
        }

        return false
    }

    override fun toString(): String {
        if (type == Type.WITNESS) {
            return Bech32.encode(network.addressSegwitHrp, hash)
        }

        val addressBytes = ByteArray(1 + hash.size + 4)
        if (type == Type.P2PKH) {
            addressBytes[0] = network.addressHeader.toByte()
        } else {
            addressBytes[0] = network.scriptAddressHeader.toByte()
        }

        System.arraycopy(hash, 0, addressBytes, 1, hash.size)
        val digest = Utils.doubleDigest(addressBytes, 0, hash.size + 1)
        System.arraycopy(digest, 0, addressBytes, hash.size + 1, 4)

        return Base58.encode(addressBytes)
    }
}
