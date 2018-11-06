package io.horizontalsystems.bitcoinkit.scripts

import io.horizontalsystems.bitcoinkit.models.Address
import io.horizontalsystems.bitcoinkit.models.AddressType
import io.horizontalsystems.bitcoinkit.models.SegWitAddress

class ScriptBuilder {

    fun lockingScript(address: Address): ByteArray {
        val data = mutableListOf<ByteArray>()

        if (address is SegWitAddress) {
            data.add(byteArrayOf(address.version.toByte()))
        }

        data.add(address.hash)

        return when (address.type) {
            AddressType.P2PKH -> OpCodes.p2pkhStart + OpCodes.push(data[0]) + OpCodes.p2pkhEnd
            AddressType.P2SH -> OpCodes.p2pshStart + OpCodes.push(data[0]) + OpCodes.p2pshEnd
            AddressType.WITNESS -> OpCodes.push(data[0][0].toInt()) + OpCodes.push(data[1])
        }
    }

    fun unlockingScript(params: List<ByteArray>): ByteArray {
        var unlockingScript = byteArrayOf()
        params.forEach {
            unlockingScript += OpCodes.push(it)
        }
        return unlockingScript
    }

}
