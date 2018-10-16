package bitcoin.wallet.kit.utils

import bitcoin.wallet.kit.hdwallet.Address
import bitcoin.wallet.kit.hdwallet.SegWitAddress
import bitcoin.walllet.kit.crypto.Bech32
import bitcoin.walllet.kit.exceptions.AddressFormatException

abstract class Bech32AddressConverter {
    abstract fun convert(hrp: String, addressString: String): Address?
    abstract fun convert(hrp: String, program: ByteArray): Address?
}

class SegwitAddressConverter : Bech32AddressConverter() {
    override fun convert(hrp: String, addressString: String): SegWitAddress {
        val decoded = Bech32.decode(addressString)
        if (decoded.hrp != hrp) {
            throw AddressFormatException("Address HRP ${decoded.hrp} is not correct")
        }

        val bytes = decoded.data
        val string = Bech32.encode(hrp, bytes)
        val program = Bech32.convertBits(bytes, 1, bytes.size - 1, 5, 8, false)

        return SegWitAddress(string, program)
    }

    override fun convert(hrp: String, program: ByteArray): SegWitAddress {
        val version = byteArrayOf(0)
        val bytes = version + Bech32.convertBits(program, 0, program.size, 8, 5, true)
        val string = Bech32.encode(hrp, bytes)

        return SegWitAddress(string, program)
    }
}
