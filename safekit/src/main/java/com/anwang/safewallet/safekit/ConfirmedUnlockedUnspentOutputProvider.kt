package com.anwang.safewallet.safekit

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.managers.IUnspentOutputProvider
import io.horizontalsystems.bitcoincore.storage.UnspentOutput

class ConfirmedUnlockedUnspentOutputProvider(private val storage: IStorage, private val confirmationsThreshold: Int) : IUnspentOutputProvider {

    override fun getSpendableUtxo(): List<UnspentOutput> {
        val lastBlockHeight = storage.lastBlock()?.height ?: 0
        return storage.getUnspentOutputs().filter { isOutputConfirmed(it, lastBlockHeight) }
    }

    private fun isOutputConfirmed(unspentOutput: UnspentOutput, lastBlockHeight: Int): Boolean {
        val block = unspentOutput.block ?: return false;
        val unlockedHeight = unspentOutput.output.unlockedHeight ?: return false;

        return  ( block.height <= lastBlockHeight - confirmationsThreshold + 1
                && lastBlockHeight > unlockedHeight )

    }

}
