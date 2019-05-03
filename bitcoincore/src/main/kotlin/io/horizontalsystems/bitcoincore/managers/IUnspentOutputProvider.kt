package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.storage.UnspentOutput

interface IUnspentOutputProvider {
    fun getUnspentOutputs(): List<UnspentOutput>
}
