package io.horizontalsystems.litecoinkit.mweb

import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

/**
 * MWEB stealth address (ltcmweb1... / tmweb1...).
 *
 * MWEB addresses are NOT on-chain locking scripts — they live in the extension block.
 * [lockingScript] and [lockingScriptPayload] are empty; all canonical chain spending
 * goes through peg-in/peg-out kernels handled separately.
 */
class MwebAddress(
    override val stringValue: String,
    val scanPubKey: ByteArray,
    val spendPubKey: ByteArray
) : Address {

    override val scriptType: ScriptType = ScriptType.UNKNOWN

    override val lockingScriptPayload: ByteArray = byteArrayOf()

    override val lockingScript: ByteArray = byteArrayOf()
}