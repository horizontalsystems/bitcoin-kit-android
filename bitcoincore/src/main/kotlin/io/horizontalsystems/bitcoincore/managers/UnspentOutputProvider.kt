package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.core.PluginManager
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.storage.UnspentOutput

class UnspentOutputProvider(
    private val storage: IStorage,
    private val confirmationsThreshold: Int = 6,
    val pluginManager: PluginManager
) : IUnspentOutputProvider {

    override fun getSpendableUtxo(): List<UnspentOutput> {
        return allUtxo().filter {
            pluginManager.isSpendable(it) && it.transaction.status == Transaction.Status.RELAYED
        }
    }

    private fun getUnspendableUtxo(): List<UnspentOutput> {
        return allUtxo().filter {
            !pluginManager.isSpendable(it) || it.transaction.status != Transaction.Status.RELAYED
        }
    }

    fun getBalance(): BalanceInfo {
        val spendable = getSpendableUtxo().sumOf { it.output.value }
        val unspendable = getUnspendableUtxo().sumOf { it.output.value }

        return BalanceInfo(spendable, unspendable)
    }

    // Only confirmed spendable outputs
    fun getConfirmedSpendableUtxo(): List<UnspentOutput> {
        val lastBlockHeight = storage.lastBlock()?.height ?: 0

        return getSpendableUtxo().filter {
            val block = it.block ?: return@filter false
            return@filter block.height <= lastBlockHeight - confirmationsThreshold + 1
        }
    }

    private fun allUtxo(): List<UnspentOutput> {
        val unspentOutputs = storage.getUnspentOutputs()

        if (confirmationsThreshold == 0) return unspentOutputs

        val lastBlockHeight = storage.lastBlock()?.height ?: 0

        return unspentOutputs.filter {
            // If a transaction is an outgoing transaction, then it can be used
            // even if it's not included in a block yet
            if (it.transaction.isOutgoing) {
                return@filter true
            }

            // If a transaction is an incoming transaction, then it can be used
            // only if it's included in a block and has enough number of confirmations
            val block = it.block ?: return@filter false
            if (block.height <= lastBlockHeight - confirmationsThreshold + 1) {
                return@filter true
            }

            false
        }
    }
}
