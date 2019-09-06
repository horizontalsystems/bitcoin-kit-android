package io.horizontalsystems.bitcoincash.managers

import io.horizontalsystems.bitcoincore.managers.IAddressSelector
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

class BitcoinCashAddressSelector : IAddressSelector {
    override fun getAddressVariants(addressConverter: IAddressConverter, pubKey: PublicKey): List<String> {
        val legacyAddress = addressConverter.convert(pubKey.publicKeyHash, ScriptType.P2PKH).string
        return listOf(legacyAddress)
    }
}
