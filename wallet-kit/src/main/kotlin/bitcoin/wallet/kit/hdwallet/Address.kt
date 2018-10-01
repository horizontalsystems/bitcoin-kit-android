package bitcoin.wallet.kit.hdwallet

import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.walllet.kit.crypto.Base58
import bitcoin.walllet.kit.utils.Utils

class Address {
    enum class Type {
        P2PKH, // Pay to public key hash
        P2SH   // Pay to script hash
    }

    private val type: Type
    private val hash: ByteArray
    private val network: NetworkParameters

    constructor(type: Type, hash: ByteArray, network: NetworkParameters) {
        this.type = type
        this.hash = hash
        this.network = network
    }

    // Returns the address as a Base58-encoded string with a 1-byte version and a 4-byte checksum
    override fun toString(): String {
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
