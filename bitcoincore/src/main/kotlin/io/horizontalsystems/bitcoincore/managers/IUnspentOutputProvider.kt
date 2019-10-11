package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.storage.UnspentOutput

interface IUnspentOutputProvider {
    fun getSpendableUtxo(): List<UnspentOutput>
}
