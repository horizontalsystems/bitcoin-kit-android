package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.storage.UnspentOutput

class UnspentOutputProvider(private val storage: IStorage, private val confirmationsThreshold: Int = 6) {

    fun allUnspentOutputs(): List<UnspentOutput> {
        val lastBlockHeight = storage.lastBlock()?.height ?: 0

        return storage.getUnspentOutputs().filter {
            if (it.transaction.isOutgoing) {
                return@filter true
            }

            val block = it.block ?: return@filter false
            if (block.height <= lastBlockHeight - confirmationsThreshold + 1) {
                return@filter true
            }

            false
        }

    }

    fun getBalance() = allUnspentOutputs().map { it.output.value }.sum()
}
