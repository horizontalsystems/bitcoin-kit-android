package io.horizontalsystems.litecoinkit

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.core.IUnspendableOutputRule
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.scripts.OP_8

/**
 * Outputs of a HogEx transaction, the transaction that closes each block's
 * MWEB extension block and pays out peg-outs, are spendable only after
 * [PEGOUT_MATURITY] confirmations (Litecoin consensus). A HogEx transaction
 * is recognised by its first output, which locks the MWEB pool with a
 * witness version 8 program: `OP_8 <32 bytes>`.
 */
class HogExOutputMaturityRule : IUnspendableOutputRule {

    override fun isSpendable(unspentOutput: UnspentOutput, lastBlockHeight: Int, storage: IStorage): Boolean {
        val outputs = storage.getTransactionOutputs(unspentOutput.transaction)
        val first = outputs.minByOrNull { it.index } ?: return true
        if (!isHogAddrScript(first.lockingScript)) return true

        val height = unspentOutput.block?.height ?: return false
        return lastBlockHeight - height + 1 >= PEGOUT_MATURITY
    }

    companion object {
        const val PEGOUT_MATURITY = 6

        fun isHogAddrScript(script: ByteArray) =
            script.size == 34 && script[0].toInt() == OP_8 && script[1].toInt() == 0x20
    }
}
