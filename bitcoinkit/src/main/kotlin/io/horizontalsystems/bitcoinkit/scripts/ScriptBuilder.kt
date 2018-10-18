package io.horizontalsystems.bitcoinkit.scripts

import io.horizontalsystems.bitcoinkit.models.Address
import io.horizontalsystems.bitcoinkit.models.AddressType
import io.horizontalsystems.bitcoinkit.models.SegWitAddress

class ScriptBuilder {

    private val p2pkhStart = byteArrayOf(OP_DUP.toByte(), OP_HASH160.toByte())
    private val p2pkhEnd = byteArrayOf(OP_EQUALVERIFY.toByte(), OP_CHECKSIG.toByte())

    private val p2pshStart = byteArrayOf(OP_HASH160.toByte())
    private val p2pshEnd = byteArrayOf(OP_EQUAL.toByte())

    fun lockingScript(address: Address): ByteArray {
        val data = mutableListOf<ByteArray>()

        if (address is SegWitAddress) {
            data.add(byteArrayOf(address.version.toByte()))
            data.add(address.hash)
        } else {
            data.add(address.hash)
        }

        return when (address.type) {
            AddressType.P2PKH -> p2pkhStart + OpCodes.push(data[0]) + p2pkhEnd
            AddressType.P2SH -> p2pshStart + OpCodes.push(data[0]) + p2pshEnd
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
