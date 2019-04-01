package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoinkit.utils.IAddressConverter

interface IAddressSelector {
    fun getAddressVariants(addressConverter: IAddressConverter, pubKey: PublicKey): List<String>
}

class BitcoinAddressSelector : IAddressSelector {
    override fun getAddressVariants(addressConverter: IAddressConverter, pubKey: PublicKey): List<String> {
        val wpkhShAddress = addressConverter.convert(pubKey.scriptHashP2WPKH, ScriptType.P2SH).string
        return listOf(wpkhShAddress, pubKey.publicKeyHex)
    }
}

class BitcoinCashAddressSelector : IAddressSelector {
    override fun getAddressVariants(addressConverter: IAddressConverter, pubKey: PublicKey): List<String> {
        val legacyAddress = addressConverter.convert(pubKey.publicKeyHash, ScriptType.P2PKH).string
        return listOf(legacyAddress)
    }
}
