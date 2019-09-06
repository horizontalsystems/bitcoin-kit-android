package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.managers.IAddressSelector
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

class BitcoinAddressSelector : IAddressSelector {
    override fun getAddressVariants(addressConverter: IAddressConverter, pubKey: PublicKey): List<String> {
        val wpkhShAddress = addressConverter.convert(pubKey.scriptHashP2WPKH, ScriptType.P2SH).string
        return listOf(wpkhShAddress, pubKey.publicKeyHash.toHexString())
    }
}
