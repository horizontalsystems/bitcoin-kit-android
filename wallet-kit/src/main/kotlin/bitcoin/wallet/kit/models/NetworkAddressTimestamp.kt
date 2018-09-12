package bitcoin.wallet.kit.models

import bitcoin.walllet.kit.io.BitcoinInput
import java.io.IOException

class NetworkAddressTimestamp @Throws(IOException::class) constructor(input: BitcoinInput) {

    /**
     * uint32
     */
    var timestamp: Long = 0

    var address: NetworkAddress

    init {
        timestamp = input.readUnsignedInt()
        address = NetworkAddress(input, true)
    }
}
