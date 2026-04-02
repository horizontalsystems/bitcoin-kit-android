package io.horizontalsystems.litecoinkit.mweb

import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException
import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

/**
 * Address converter for MWEB stealth addresses (ltcmweb1... / tmweb1...).
 *
 * Register this via [bitcoinCore.prependAddressConverter] so that MWEB addresses
 * are recognised by [validateAddress] and [send] before the regular SegWit converter
 * is tried.
 */
class MwebAddressConverter(private val hrp: String) : IAddressConverter {

    override fun convert(addressString: String): Address {
        if (!addressString.lowercase().startsWith(hrp)) {
            throw AddressFormatException("Not an MWEB address (expected HRP \"$hrp\")")
        }
        val (scanPubKey, spendPubKey) = MwebBech32.decode(hrp, addressString)
        return MwebAddress(addressString.lowercase(), scanPubKey, spendPubKey)
    }

    override fun convert(lockingScriptPayload: ByteArray, scriptType: ScriptType): Address {
        throw AddressFormatException("Cannot derive MWEB address from locking script")
    }

    override fun convert(publicKey: PublicKey, scriptType: ScriptType): Address {
        throw AddressFormatException("Cannot derive MWEB address from PublicKey — use LitecoinKit.mwebAddress()")
    }
}