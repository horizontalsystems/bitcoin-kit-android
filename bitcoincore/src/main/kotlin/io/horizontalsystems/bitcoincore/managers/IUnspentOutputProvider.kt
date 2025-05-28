package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.storage.UtxoFilters

interface IUnspentOutputProvider {
    fun getSpendableUtxo(filters: UtxoFilters): List<UnspentOutput>
}
