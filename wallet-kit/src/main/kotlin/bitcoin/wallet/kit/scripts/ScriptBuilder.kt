package bitcoin.wallet.kit.scripts

import bitcoin.wallet.kit.hdwallet.Address

class ScriptBuilder {

    private val p2pkhStart = byteArrayOf(OP_DUP.toByte(), OP_HASH160.toByte())
    private val p2pkhEnd = byteArrayOf(OP_EQUALVERIFY.toByte(), OP_CHECKSIG.toByte())

    private val p2pshStart = byteArrayOf(OP_HASH160.toByte())
    private val p2pshEnd = byteArrayOf(OP_EQUAL.toByte())

    fun lockingScript(address: Address): ByteArray {
        val data = mutableListOf<ByteArray>()

        if (address.type == Address.Type.WITNESS) {
            data.add(byteArrayOf(0x00)) //TODO take VERSION from address object
        }
        data.add(address.hash)

        return when (address.type) {
            Address.Type.P2PKH -> p2pkhStart + OpCodes.push(data[0]) + p2pkhEnd
            Address.Type.P2SH -> p2pshStart + OpCodes.push(data[0]) + p2pshEnd
            Address.Type.WITNESS -> OpCodes.push(data[0][0].toInt()) + OpCodes.push(data[1])
        }
    }

    fun unlockingScript(params: List<ByteArray>): ByteArray {
        var unlockingScript = byteArrayOf()
        params.forEach {
            unlockingScript += OpCodes.push(it)
        }
        return unlockingScript
    }

    open class ScriptBuilderException : Exception()
    class UnknownType : ScriptBuilderException()

}
