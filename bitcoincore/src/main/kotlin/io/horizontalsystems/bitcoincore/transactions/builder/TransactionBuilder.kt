package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.InputToSign
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptBuilder
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

class TransactionBuilder(
        private val scriptBuilder: ScriptBuilder,
        private val inputSigner: InputSigner,
        private val outputSetter: OutputSetter,
        private val inputSetter: InputSetter,
        private val signer: TransactionSigner,
        private val lockTimeSetter: LockTimeSetter
) {

    fun buildTransaction(toAddress: String, value: Long, feeRate: Int, senderPay: Boolean, extraData: Map<String, Map<String, Any>>): FullTransaction {
        val mutableTransaction = MutableTransaction(scriptBuilder)

        outputSetter.setOutputs(mutableTransaction, toAddress, value, extraData)
        inputSetter.setInputs(mutableTransaction, feeRate, senderPay)
        lockTimeSetter.setLockTime(mutableTransaction)
        signer.sign(mutableTransaction)

        return mutableTransaction.build()
    }

    fun buildTransaction(unspentOutput: UnspentOutput, address: Address, fee: Long, lastBlockHeight: Long, signatureScriptFunction: (ByteArray, ByteArray) -> ByteArray): FullTransaction {

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
        val transaction = Transaction(version = 1, lockTime = lastBlockHeight)

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
