package io.horizontalsystems.bitcoinkit.segwit

import io.horizontalsystems.bitcoincore.core.IAddressKeyHashConverter
import io.horizontalsystems.bitcoincore.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.Utils

class SegWitBech32KeyHashConverter : IAddressKeyHashConverter {

    override fun convert(keyHash: ByteArray, type: Int): ByteArray {
        return when (type) {
            ScriptType.P2WPKH -> OpCodes.scriptWPKH(keyHash)
            ScriptType.P2WPKHSH -> Utils.sha256Hash160(OpCodes.scriptWPKH(keyHash))
            else -> keyHash
        }
    }

}
