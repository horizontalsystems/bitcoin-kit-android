package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoinkit.utils.AddressConverter

interface IAddressSelector {
    fun getAddressVariants(pubKey: PublicKey): List<String>
}

class BitcoinAddressSelector(private val addressConverter: AddressConverter) : IAddressSelector {
    override fun getAddressVariants(pubKey: PublicKey): List<String> {
        val wpkhShAddress = addressConverter.convert(pubKey.scriptHashP2WPKH, ScriptType.P2SH).string
        return listOf(wpkhShAddress, pubKey.publicKeyHex)
    }
}

class BitcoinCashAddressSelector(private val addressConverter: AddressConverter) : IAddressSelector {
    override fun getAddressVariants(pubKey: PublicKey): List<String> {
        val legacyAddress = addressConverter.convert(pubKey.publicKeyHash, ScriptType.P2PKH).string
        return listOf(legacyAddress)
    }
}
