package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.core.PluginManager
import io.horizontalsystems.bitcoincore.storage.UnspentOutput

class UnspentOutputProvider(private val storage: IStorage, private val confirmationsThreshold: Int = 6, val pluginManager: PluginManager) : IUnspentOutputProvider {
    override fun getUnspentOutputs(): List<UnspentOutput> {
        return getConfirmedUTXO().filter {
            pluginManager.isSpendable(it.output)
        }
    }

    fun getBalance(): BitcoinBalance {
        val spendable = getUnspentOutputs().map { it.output.value }.sum()
        val unspendable = getUnspendableUnspentOutputs().map { it.output.value }.sum()

        return BitcoinBalance(spendable, unspendable)
    }

    private fun getConfirmedUTXO(): List<UnspentOutput> {
        val unspentOutputs = storage.getUnspentOutputs()

        if (confirmationsThreshold == 0) return unspentOutputs

        val lastBlockHeight = storage.lastBlock()?.height ?: 0

        return unspentOutputs.filter {
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

    private fun getUnspendableUnspentOutputs(): List<UnspentOutput> {
        return getConfirmedUTXO().filter {
            !pluginManager.isSpendable(it.output)
        }
    }
}

data class BitcoinBalance(val spendable: Long, val unspendable: Long)
