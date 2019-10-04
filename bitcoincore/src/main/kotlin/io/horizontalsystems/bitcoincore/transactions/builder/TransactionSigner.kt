package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptBuilder
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

class TransactionSigner(private val scriptBuilder: ScriptBuilder, private val inputSigner: InputSigner) {

    fun sign(mutableTransaction: MutableTransaction) {
        val inputsToSign = mutableTransaction.inputsToSign
        val outputs = mutableTransaction.outputs

        inputsToSign.forEachIndexed { index, inputToSign ->
            val previousOutput = inputToSign.previousOutput
            val publicKey = inputToSign.previousOutputPublicKey
            val sigScriptData = inputSigner.sigScriptData(mutableTransaction.transaction, inputsToSign, outputs, index)

            when (previousOutput.scriptType) {
                ScriptType.P2PKH -> {
                    inputToSign.input.sigScript = scriptBuilder.unlockingScript(sigScriptData)
                }

                ScriptType.P2WPKH -> {
                    mutableTransaction.transaction.segwit = true
                    inputToSign.input.witness = sigScriptData
                }

                ScriptType.P2WPKHSH -> {
                    mutableTransaction.transaction.segwit = true
                    val witnessProgram = OpCodes.scriptWPKH(publicKey.publicKeyHash)

                    inputToSign.input.sigScript = scriptBuilder.unlockingScript(listOf(witnessProgram))
                    inputToSign.input.witness = sigScriptData
                }

                else -> throw TransactionBuilder.BuilderException.NotSupportedScriptType()
            }
        }
    }

}
