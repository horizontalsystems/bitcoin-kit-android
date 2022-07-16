package com.anwang.safewallet.safekit

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.extensions.toHexString
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
        val reserve = unspentOutput.output.reserve;
        if ( reserve != null ){
            if ( reserve.toHexString() != "73616665"  // 普通交易
                // coinbase 收益
                && reserve.toHexString() != "7361666573706f730100c2f824c4364195b71a1fcfa0a28ebae20f3501b21b08ae6d6ae8a3bca98ad9d64136e299eba2400183cd0a479e6350ffaec71bcaf0714a024d14183c1407805d75879ea2bf6b691214c372ae21939b96a695c746a6"
                // safe备注，也是属于safe交易
                && !reserve.toHexString().startsWith("736166650100c9dcee22bb18bd289bca86e2c8bbb6487089adc9a13d875e538dd35c70a6bea42c0100000a02010012")){
                return false;
            }
        }
        return  ( block.height <= lastBlockHeight - confirmationsThreshold + 1
                && lastBlockHeight > unlockedHeight )

    }

    fun getLockUxto(): List<UnspentOutput> {
        return storage.getUnspentOutputs().filter { isLock(it) }
    }

    private fun isLock(unspentOutput: UnspentOutput): Boolean {
        val unlockedHeight = unspentOutput.output.unlockedHeight ?: return false;
        return unlockedHeight > 0;
    }

}
