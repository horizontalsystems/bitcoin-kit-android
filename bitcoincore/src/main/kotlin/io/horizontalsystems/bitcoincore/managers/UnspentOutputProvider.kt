package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.core.PluginManager
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.storage.UnspentOutput

class UnspentOutputProvider(private val storage: IStorage, private val confirmationsThreshold: Int = 6, val pluginManager: PluginManager) : IUnspentOutputProvider {
    override fun getSpendableUtxo(): List<UnspentOutput> {
        val lastBlockHeight = storage.lastBlock()?.height ?: 0
        return getConfirmedUtxo().filter {
            val unlockedHeight = it.output.unlockedHeight;
            if ( unlockedHeight != null && unlockedHeight > lastBlockHeight){
                return@filter false
            }
            pluginManager.isSpendable(it)
        }
    }

    fun getBalance(): BalanceInfo {
        val spendable = getSpendableUtxo().map { it.output.value }.sum()
        val unspendable = getUnspendableUtxo().map { it.output.value }.sum()
        return BalanceInfo(spendable, unspendable)
    }

    private fun getConfirmedUtxo(): List<UnspentOutput> {
        var unspentOutputs = storage.getUnspentOutputs()
        if (confirmationsThreshold == 0) return unspentOutputs
        val lastBlockHeight = storage.lastBlock()?.height ?: 0
        return unspentOutputs.filter {
            if (it.transaction.isOutgoing) {
                return@filter true
            }
            val block = it.block ?: return@filter false

            // - Update for Safe-Asset reserve
            val reserve = it.output.reserve;
            if ( reserve != null ){
                if ( reserve.toHexString() != "73616665"  // 普通交易
                    // coinbase 收益
                    && reserve.toHexString() != "7361666573706f730100c2f824c4364195b71a1fcfa0a28ebae20f3501b21b08ae6d6ae8a3bca98ad9d64136e299eba2400183cd0a479e6350ffaec71bcaf0714a024d14183c1407805d75879ea2bf6b691214c372ae21939b96a695c746a6"
                    // safe备注，也是属于safe交易
                    && !reserve.toHexString().startsWith("736166650100c9dcee22bb18bd289bca86e2c8bbb6487089adc9a13d875e538dd35c70a6bea42c0100000a02010012")){
                    return@filter false
                }
            }
            /////////////////////////////////

            if (block.height <= lastBlockHeight - confirmationsThreshold + 1) {
                return@filter true
            }
            false
        }
    }

    private fun getUnspendableUtxo(): List<UnspentOutput> {
        val lastBlockHeight = storage.lastBlock()?.height ?: 0
        return getConfirmedUtxo().filter {
            val unlockedHeight = it.output.unlockedHeight;
            if ( unlockedHeight != null && unlockedHeight > lastBlockHeight){
                return@filter true
            }
            !pluginManager.isSpendable(it)
        }
    }



}
