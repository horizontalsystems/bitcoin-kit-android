package io.horizontalsystems.bitcoincore.models

import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException
import io.horizontalsystems.bitcoincore.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

enum class AddressType {
    P2PKH,  // Pay to public key hash
    P2SH,   // Pay to script hash
    WITNESS // Pay to witness hash
}

abstract class Address {
    lateinit var type: AddressType
    lateinit var hash: ByteArray
    lateinit var string: String

    val scriptType: ScriptType
        get() = when (type) {
            AddressType.P2PKH -> ScriptType.P2PKH
            AddressType.P2SH -> ScriptType.P2SH
            AddressType.WITNESS ->
                if (hash.size == 20) ScriptType.P2WPKH else ScriptType.P2WSH
        }

    open val lockingScript: ByteArray
        get() = when (type) {
            AddressType.P2PKH -> OpCodes.p2pkhStart + OpCodes.push(hash) + OpCodes.p2pkhEnd
            AddressType.P2SH -> OpCodes.p2pshStart + OpCodes.push(hash) + OpCodes.p2pshEnd
            else -> throw AddressFormatException("Unknown Address Type")
        }
}

class LegacyAddress(addressString: String, bytes: ByteArray, type: AddressType) : Address() {
    init {
        this.type = type
        this.hash = bytes
        this.string = addressString
    }
}

class SegWitAddress(addressString: String, bytes: ByteArray, type: AddressType, val version: Int) : Address() {
    init {
        this.type = type
        this.hash = bytes
        this.string = addressString
    }

    override val lockingScript: ByteArray
        get() = OpCodes.push(version) + OpCodes.push(hash)
}

class CashAddress(addressString: String, bytes: ByteArray, type: AddressType) : Address() {
    init {
        this.type = type
        this.hash = bytes
        this.string = addressString
    }
}
