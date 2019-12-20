package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

class TransactionSizeCalculator {
    val signatureLength = 72 + 1      // signature length + pushByte
    val pubKeyLength = 33 + 1         // pubKey length + pushByte
    private val p2wpkhShLength = 22 + 1       // 0014<20-byte-script-hash> + pushByte

    private val legacyTx = 16 + 4 + 4 + 16    // 40 Version + number of inputs + number of outputs + locktime
    private val legacyWitnessData = 1         // 1 Only 0x00 for legacy input
    private val witnessTx = legacyTx + 1 + 1  // 42 segwit marker + segwit flag
    private val witnessData = 1 + signatureLength + pubKeyLength  // 108 Number of stack items for input + Size of stack item 0 + Stack item 0, signature + Size of stack item 1 + Stack item 1, pubkey

    private val sigLengths = mapOf(
            ScriptType.P2PKH to signatureLength + pubKeyLength,
            ScriptType.P2PK to signatureLength,
            ScriptType.P2WPKHSH to p2wpkhShLength
    )

    private val lockingScriptSizes = mapOf(
            ScriptType.P2PK to 35,
            ScriptType.P2PKH to 25,
            ScriptType.P2SH to 23,
            ScriptType.P2WPKH to 22,
            ScriptType.P2WSH to 34,
            ScriptType.P2WPKHSH to 23
    )

    fun outputSize(scripType: ScriptType): Int {
        return 8 + 1 + getLockingScriptSize(scripType)
    }

    fun inputSize(scriptType: ScriptType): Int {
        val sigLength = sigLengths[scriptType] ?: 0

        return 32 + 4 + 1 + sigLength + 4 // PreviousOutputHex + OutputIndex + sigLength + sigScript + sequence
    }

    /**
     * Calculate size only for those inputs, which we can sign later in TransactionSigner.
     * Any other inputs will fail to sign later, so no need to calculate size here.
     */
    private fun inputSize(output: TransactionOutput): Int {
        val scriptSigLength = when (output.scriptType) {
            ScriptType.P2PK -> signatureLength
            ScriptType.P2PKH -> signatureLength + pubKeyLength
            ScriptType.P2WPKHSH -> p2wpkhShLength
            ScriptType.P2SH -> {
                output.redeemScript?.let { redeemScript ->
                    val signatureScriptFunction = output.signatureScriptFunction
                    if (signatureScriptFunction != null) {
                        // non-standard P2SH signature script
                        val emptySignature = ByteArray(signatureLength)
                        val emptyPublicKey = ByteArray(pubKeyLength)

                        signatureScriptFunction(listOf(emptySignature, emptyPublicKey)).size
                    } else {
                        // standard (signature, publicKey, redeemScript) signature script
                        signatureLength + pubKeyLength + OpCodes.push(redeemScript).size
                    }
                } ?: 0
            }
            else -> 0
        }

        return 32 + 4 + 1 + scriptSigLength + 4 // PreviousOutputHex + InputIndex + sigLength + scriptSig + sequence
    }

    fun transactionSize(previousOutputs: List<TransactionOutput>, outputs: List<ScriptType>, pluginDataOutputSize: Int): Long {
        val txIsWitness = previousOutputs.any { it.scriptType.isWitness }
        val txWeight = if (txIsWitness) witnessTx else legacyTx

        val inputWeight = previousOutputs.map { inputSize(it) * 4 + if (txIsWitness) witnessSize(it.scriptType) else 0 }.sum()
        var outputWeight = outputs.map { outputSize(it) }.sum() * 4 // to vbytes

        if (pluginDataOutputSize > 0) {
            outputWeight += outputSizeByScriptSize(pluginDataOutputSize) * 4
        }

        return toBytes(txWeight + inputWeight + outputWeight).toLong()
    }

    private fun outputSizeByScriptSize(size: Int): Int {
        return 8 + 1 + size            // spentValue + scriptLength + script
    }

    fun witnessSize(type: ScriptType): Int {  // in vbytes
        return when {
            type.isWitness -> witnessData  // We assume that only P2WPKH or P2WPKH(SH) outputs can be here
            else -> legacyWitnessData
        }

    }

    private fun toBytes(fee: Int): Int {
        return (fee / 4) + if (fee % 4 == 0) 0 else 1
    }

    private fun getLockingScriptSize(scriptType: ScriptType): Int {
        return lockingScriptSizes[scriptType] ?: 0
    }

}
