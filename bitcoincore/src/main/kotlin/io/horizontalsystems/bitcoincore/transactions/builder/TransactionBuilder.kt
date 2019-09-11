package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.InputToSign
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptBuilder
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

class TransactionBuilder(private val scriptBuilder: ScriptBuilder, private val inputSigner: InputSigner) {

    fun buildTransaction(value: Long, unspentOutputs: List<UnspentOutput>, fee: Long, senderPay: Boolean, address: Address, changeAddress: Address?): FullTransaction {

        if (fee > value && !senderPay) {
            throw BuilderException.FeeMoreThanValue()
        }

        val transaction = Transaction(version = 1, lockTime = 0)
        val inputsToSign = mutableListOf<InputToSign>()
        val outputs = mutableListOf<TransactionOutput>()

        //  add inputs
        for (unspentOutput in unspentOutputs) {
            val previousOutput = unspentOutput.output
            val transactionInput = TransactionInput(previousOutput.transactionHash, previousOutput.index.toLong())

            if (unspentOutput.output.scriptType == ScriptType.P2WPKH) {
                unspentOutput.output.keyHash = unspentOutput.output.keyHash?.drop(2)?.toByteArray()
            }

            inputsToSign.add(InputToSign(transactionInput, previousOutput, unspentOutput.publicKey))
        }

        //  calculate fee and add change output if needed
        val receivedValue = if (senderPay) value else value - fee
        val sentValue = if (senderPay) value + fee else value

        //  add output
        outputs.add(TransactionOutput(receivedValue, 0, scriptBuilder.lockingScript(address), address.scriptType, address.string, address.hash))

        if (changeAddress != null) {
            val totalValue = unspentOutputs.fold(0L) { sum, unspent -> sum + unspent.output.value }
            outputs.add(TransactionOutput(totalValue - sentValue, 1, scriptBuilder.lockingScript(changeAddress), changeAddress.scriptType, changeAddress.string, changeAddress.hash))
        }

        // sign inputs
        inputsToSign.forEachIndexed { index, inputToSign ->
            val unspentOutput = unspentOutputs[index]
            val sigScriptData = inputSigner.sigScriptData(transaction, inputsToSign, outputs, index)

            when (unspentOutput.output.scriptType) {
                ScriptType.P2PKH -> {
                    inputToSign.input.sigScript = scriptBuilder.unlockingScript(sigScriptData)
                }

                ScriptType.P2WPKH -> {
                    transaction.segwit = true
                    inputToSign.input.witness = sigScriptData
                }

                ScriptType.P2WPKHSH -> {
                    transaction.segwit = true
                    val witnessProgram = OpCodes.scriptWPKH(unspentOutput.publicKey.publicKeyHash)

                    inputToSign.input.sigScript = scriptBuilder.unlockingScript(listOf(witnessProgram))
                    inputToSign.input.witness = sigScriptData
                }

                else -> throw BuilderException.NotSupportedScriptType()
            }
        }

        transaction.status = Transaction.Status.NEW
        transaction.isMine = true
        transaction.isOutgoing = true

        return FullTransaction(transaction, inputsToSign.map { it.input }, outputs)
    }

    fun buildTransaction(unspentOutput: UnspentOutput, address: Address, fee: Long, signatureScriptFunction: (ByteArray, ByteArray) -> ByteArray): FullTransaction {

        if (unspentOutput.output.scriptType != ScriptType.P2SH) {
            throw BuilderException.NotSupportedScriptType()
        }

        if (unspentOutput.output.value < fee) {
            throw BuilderException.FeeMoreThanValue()
        }

        //  add input without unlocking scripts
        val transactionInput = TransactionInput(unspentOutput.output.transactionHash, unspentOutput.output.index.toLong())
        if (unspentOutput.output.scriptType == ScriptType.P2WPKH) {
            unspentOutput.output.keyHash = unspentOutput.output.keyHash?.drop(2)?.toByteArray()
        }
        val inputToSign = InputToSign(transactionInput, unspentOutput.output, unspentOutput.publicKey)

        //  calculate receiveValue
        val receivedValue = unspentOutput.output.value - fee

        //  add output
        val output = TransactionOutput(receivedValue, 0, scriptBuilder.lockingScript(address), address.scriptType, address.string, address.hash)

        //  build transaction
        val transaction = Transaction(version = 1, lockTime = 0)

        //  sign input
        val sigScriptData = inputSigner.sigScriptData(transaction, listOf(inputToSign), listOf(output), 0)
        inputToSign.input.sigScript = signatureScriptFunction.invoke(sigScriptData[0], sigScriptData[1])

        transaction.status = Transaction.Status.NEW
        transaction.isMine = true
        transaction.isOutgoing = false

        return FullTransaction(transaction, listOf(inputToSign.input), listOf(output))
    }

    open class BuilderException : Exception() {
        class FeeMoreThanValue : BuilderException()
        class NotSupportedScriptType : BuilderException()
    }
}
