package io.horizontalsystems.bitcoincore.models

import io.horizontalsystems.bitcoincore.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

enum class AddressType {
    PubKeyHash,
    ScriptHash
}

interface Address {
    val scriptType: ScriptType
    val lockingScriptPayload: ByteArray
    val stringValue: String
    val lockingScript: ByteArray
}

class LegacyAddress(
    override val stringValue: String,
    override val lockingScriptPayload: ByteArray,
    val type: AddressType
) : Address {

    override val scriptType: ScriptType
        get() = when (type) {
            AddressType.PubKeyHash -> ScriptType.P2PKH
            AddressType.ScriptHash -> ScriptType.P2SH
        }

    override val lockingScript: ByteArray
        get() = when (type) {
            AddressType.PubKeyHash -> OpCodes.p2pkhStart + OpCodes.push(lockingScriptPayload) + OpCodes.p2pkhEnd
            AddressType.ScriptHash -> OpCodes.p2pshStart + OpCodes.push(lockingScriptPayload) + OpCodes.p2pshEnd
        }
}

class SegWitV0Address(
    override val stringValue: String,
    override val lockingScriptPayload: ByteArray,
    val type: AddressType
) : Address {

    override val scriptType: ScriptType
        get() = when (type) {
            AddressType.PubKeyHash -> ScriptType.P2WPKH
            AddressType.ScriptHash -> ScriptType.P2WSH
        }

    override val lockingScript: ByteArray
        get() = OpCodes.push(0) + OpCodes.push(lockingScriptPayload)
}

class TaprootAddress(
    override val stringValue: String,
    override val lockingScriptPayload: ByteArray,
    val version: Int
) : Address {

    override val scriptType: ScriptType = ScriptType.P2TR

    override val lockingScript: ByteArray
        get() = OpCodes.push(version) + OpCodes.push(lockingScriptPayload)
}

class CashAddress(
    override val stringValue: String,
    override val lockingScriptPayload: ByteArray,
    val version: Int,
    val type: AddressType
) : Address {

    override val scriptType: ScriptType
        get() = when (type) {
            AddressType.PubKeyHash -> ScriptType.P2PKH
            AddressType.ScriptHash -> ScriptType.P2SH
        }

    override val lockingScript: ByteArray
        get() = when (type) {
            AddressType.PubKeyHash -> OpCodes.p2pkhStart + OpCodes.push(lockingScriptPayload) + OpCodes.p2pkhEnd
            AddressType.ScriptHash -> OpCodes.p2pshStart + OpCodes.push(lockingScriptPayload) + OpCodes.p2pshEnd
        }
}
