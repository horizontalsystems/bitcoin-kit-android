package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.scripts.ScriptType
import io.horizontalsystems.bitcoinkit.scripts.ScriptType.P2PK
import io.horizontalsystems.bitcoinkit.scripts.ScriptType.P2PKH
import io.horizontalsystems.bitcoinkit.scripts.ScriptType.P2WPKHSH

class TransactionSizeCalculator {
    private val signatureLength = 72 + 1      // signature length + pushByte
    private val pubKeyLength = 33 + 1         // pubKey length + pushByte
    private val p2wpkhShLength = 22 + 1       // 0014<20-byte-script-hash> + pushByte

    private val legacyTx = 16 + 4 + 4 + 16    // 40 Version + number of inputs + number of outputs + locktime
    private val legacyWitnessData = 1         // 1 Only 0x00 for legacy input
    private val witnessTx = legacyTx + 1 + 1  // 42 segwit marker + segwit flag
    private val witnessData = 1 + signatureLength + pubKeyLength  // 108 Number of stack items for input + Size of stack item 0 + Stack item 0, signature + Size of stack item 1 + Stack item 1, pubkey

    fun outputSize(scripType: Int): Int {
        return 8 + 1 + getLockingScriptSize(scripType)
    }

    fun inputSize(scriptType: Int): Int {
        val sigLength = when (scriptType) {
            P2PKH -> signatureLength + pubKeyLength
            P2PK -> signatureLength
            P2WPKHSH -> p2wpkhShLength
            else -> 0
        }

        return 32 + 4 + 1 + sigLength + 4 // PreviousOutputHex + OutputIndex + sigLength + sigScript + sequence
    }

    fun transactionSize(inputs: List<Int>, outputs: List<Int>): Int {
        var segwit = false
        var inputWeight = 0

        for (input in inputs) {
            if (isWitness(input)) {
                segwit = true
                break
            }
        }

        inputs.forEach { input ->
            inputWeight += inputSize(input) * 4  // to vbytes
            if (isWitness(input)) {
                inputWeight += witnessSize(input)
            }
        }

        val outputWeight = outputs.fold(0) { memo, next -> memo + outputSize(next) } * 4 // to vbytes
        val txWeight = if (segwit) witnessTx else legacyTx

        return toBytes(txWeight + inputWeight + outputWeight)
    }

    private fun witnessSize(type: Int): Int {  // in vbytes
        if (isWitness(type)) {
            return witnessData
        }

        return legacyWitnessData
    }

    private fun toBytes(fee: Int): Int {
        return (fee / 4) + if (fee % 4 == 0) 0 else 1
    }

    private fun getLockingScriptSize(scriptType: Int) = when (scriptType) {
        ScriptType.P2PK -> 35
        ScriptType.P2PKH -> 25
        ScriptType.P2SH -> 23
        ScriptType.P2WPKH -> 22
        ScriptType.P2WSH -> 34
        ScriptType.P2WPKHSH -> 23
        else -> 0
    }

    private fun isWitness(type: Int): Boolean {
        return type in arrayOf(ScriptType.P2WPKH, ScriptType.P2WSH, ScriptType.P2WPKHSH)
    }
}
