package io.horizontalsystems.bitcoincore

import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

class DustCalculator(dustRelayTxFee: Int, val sizeCalculator: TransactionSizeCalculator) {
    val minFeeRate = dustRelayTxFee / 1000

    fun dust(type: Int): Int {
        // https://github.com/bitcoin/bitcoin/blob/c536dfbcb00fb15963bf5d507b7017c241718bf6/src/policy/policy.cpp#L18

        var size = sizeCalculator.outputSize(type)

        if (sizeCalculator.isWitness(type)) {
            size += sizeCalculator.inputSize(ScriptType.P2WPKH) + sizeCalculator.witnessSize(ScriptType.P2WPKH) / 4
        } else {
            size += sizeCalculator.inputSize(ScriptType.P2PKH)
        }

        return size * minFeeRate
    }
}
