package bitcoin.wallet.kit.utils

import bitcoin.wallet.kit.hdwallet.Address
import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.wallet.kit.scripts.ScriptType

class AddressConverter(private val network: NetworkParameters) {

    fun convert(bytes: ByteArray, type: Int = ScriptType.P2PKH): Address {
        if (type == ScriptType.P2PKH) {
            return Address(Address.Type.P2PKH, bytes, network)
        }

        TODO("need to implement address conversion for script type: $type")
    }
}
