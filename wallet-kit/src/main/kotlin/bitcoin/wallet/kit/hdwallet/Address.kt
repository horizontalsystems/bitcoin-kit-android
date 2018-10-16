package bitcoin.wallet.kit.hdwallet

import bitcoin.wallet.kit.scripts.ScriptParser
import bitcoin.wallet.kit.scripts.ScriptType

enum class AddressType {
    P2PKH,  // Pay to public key hash
    P2SH,   // Pay to script hash
    WITNESS // Pay to witness hash
}

abstract class Address {
    lateinit var type: AddressType
    lateinit var hash: ByteArray
    lateinit var string: String

    val scriptType: Int
        get() = when (type) {
            AddressType.P2PKH -> ScriptType.P2PKH
            AddressType.P2SH -> ScriptType.P2SH
            AddressType.WITNESS ->
                if (hash.size == ScriptParser.WITNESS_PKH_LENGTH) ScriptType.P2WPKH else ScriptType.P2WSH
        }
}

class LegacyAddress(addressString: String, bytes: ByteArray, type: AddressType) : Address() {
    init {
        this.string = addressString
        this.hash = bytes
        this.type = type
    }
}

class SegWitAddress(addressString: String, program: ByteArray) : Address() {
    val version: Int = 0

    init {
        this.type = AddressType.WITNESS
        this.string = addressString
        this.hash = program
    }
}
