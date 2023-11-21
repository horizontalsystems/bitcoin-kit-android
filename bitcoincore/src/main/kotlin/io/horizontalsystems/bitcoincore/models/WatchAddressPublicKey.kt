package io.horizontalsystems.bitcoincore.models

import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

class WatchAddressPublicKey(
    data: ByteArray,
    scriptType: ScriptType
) : PublicKey() {
    init {
        path = "WatchAddressPublicKey"
        when (scriptType) {
            ScriptType.P2PKH,
            ScriptType.P2WPKH -> {
                publicKeyHash = data
            }

            ScriptType.P2SH,
            ScriptType.P2WSH,
            ScriptType.P2WPKHSH -> {
                scriptHashP2WPKH = data
            }

            ScriptType.P2TR -> {
                convertedForP2TR = data
            }

            ScriptType.P2PK -> {
                publicKey = data
            }

            ScriptType.NULL_DATA,
            ScriptType.UNKNOWN -> {
                Unit // Not supported yet
            }
        }
    }
}
