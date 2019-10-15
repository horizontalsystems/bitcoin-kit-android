package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

class TransactionSigner(private val inputSigner: InputSigner) {

    fun sign(mutableTransaction: MutableTransaction) {
        val inputsToSign = mutableTransaction.inputsToSign
        val outputs = mutableTransaction.outputs

        inputsToSign.forEachIndexed { index, inputToSign ->
            val previousOutput = inputToSign.previousOutput
            val publicKey = inputToSign.previousOutputPublicKey
            val sigScriptData = inputSigner.sigScriptData(mutableTransaction.transaction, inputsToSign, outputs, index)

            when (previousOutput.scriptType) {
                ScriptType.P2PKH -> {
                    inputToSign.input.sigScript = signatureScript(sigScriptData)
                }

                ScriptType.P2WPKH -> {
                    mutableTransaction.transaction.segwit = true
                    inputToSign.input.witness = sigScriptData
                }

                ScriptType.P2WPKHSH -> {
                    mutableTransaction.transaction.segwit = true
                    val witnessProgram = OpCodes.scriptWPKH(publicKey.publicKeyHash)

                    inputToSign.input.sigScript = signatureScript(listOf(witnessProgram))
                    inputToSign.input.witness = sigScriptData
                }

                ScriptType.P2SH -> {
                    val redeemScript = previousOutput.redeemScript ?: throw NoRedeemScriptException()
                    val signatureScriptFunction = previousOutput.signatureScriptFunction

                    if (signatureScriptFunction != null) {
                        // non-standard P2SH signature script
                        inputToSign.input.sigScript = signatureScriptFunction(sigScriptData)
                    } else {
                        // standard (signature, publicKey, redeemScript) signature script
                        inputToSign.input.sigScript = signatureScript(sigScriptData + redeemScript)
                    }
                }

                else -> throw TransactionBuilder.BuilderException.NotSupportedScriptType()
            }
        }
    }

    private fun signatureScript(params: List<ByteArray>): ByteArray {
        return params.fold(byteArrayOf()) { acc, bytes -> acc + OpCodes.push(bytes) }
    }
}

class NoRedeemScriptException : Exception()
