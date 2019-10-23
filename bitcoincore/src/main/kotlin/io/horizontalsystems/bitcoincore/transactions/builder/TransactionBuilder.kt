package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.UnspentOutput

class TransactionBuilder(
        private val outputSetter: OutputSetter,
        private val inputSetter: InputSetter,
        private val signer: TransactionSigner,
        private val lockTimeSetter: LockTimeSetter
) {

    fun buildTransaction(toAddress: String, value: Long, feeRate: Int, senderPay: Boolean, pluginData: Map<Byte, IPluginData>): FullTransaction {
        val mutableTransaction = MutableTransaction()

        outputSetter.setOutputs(mutableTransaction, toAddress, value, pluginData)
        inputSetter.setInputs(mutableTransaction, feeRate, senderPay)
        lockTimeSetter.setLockTime(mutableTransaction)
        signer.sign(mutableTransaction)

        return mutableTransaction.build()
    }

    fun buildTransaction(unspentOutput: UnspentOutput, toAddress: String, feeRate: Int): FullTransaction {
        val mutableTransaction = MutableTransaction(false)

        outputSetter.setOutputs(mutableTransaction, toAddress, unspentOutput.output.value, mapOf())
        inputSetter.setInputs(mutableTransaction, unspentOutput, feeRate)
        lockTimeSetter.setLockTime(mutableTransaction)
        signer.sign(mutableTransaction)

        return mutableTransaction.build()
    }

    open class BuilderException : Exception() {
        class FeeMoreThanValue : BuilderException()
        class NotSupportedScriptType : BuilderException()
    }
}
