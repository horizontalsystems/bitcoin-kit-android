package io.horizontalsystems.bitcoincore

import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

/**
 * Calculates the minimum amount of BTC or "dust" required to broadcast a transaction and pay miner fees.
 * @param dustRelayTxFee
 * @param sizeCalculator
 *
 */
class DustCalculator(dustRelayTxFee: Int, val sizeCalculator: TransactionSizeCalculator) {
    val minFeeRate = dustRelayTxFee / 1000
/**
 *@param type The ScriptType (Ex: P2PKH, P2WPKH, P2SH, etc.)
 *@return The minimum amount of satoshis required to make a transaction.
 */
    fun dust(type: ScriptType): Int {
        // https://github.com/bitcoin/bitcoin/blob/c536dfbcb00fb15963bf5d507b7017c241718bf6/src/policy/policy.cpp#L18

        var size = sizeCalculator.outputSize(type)

        size += if (type.isWitness) {
            sizeCalculator.inputSize(ScriptType.P2WPKH) + sizeCalculator.witnessSize(ScriptType.P2WPKH) / 4
        } else {
            sizeCalculator.inputSize(ScriptType.P2PKH)
        }

        return size * minFeeRate
    }
}
