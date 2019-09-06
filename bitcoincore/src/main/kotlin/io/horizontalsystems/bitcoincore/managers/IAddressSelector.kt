package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

interface IAddressSelector {
    fun getAddressVariants(addressConverter: IAddressConverter, pubKey: PublicKey): List<String>
}
