package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.core.IRecipientSetter
import io.horizontalsystems.bitcoincore.models.TransactionDataSortType
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.UnspentOutput

class TransactionBuilder(
    private val recipientSetter: IRecipientSetter,
    private val outputSetter: OutputSetter,
    private val inputSetter: InputSetter,
    private val signer: TransactionSigner,
    private val lockTimeSetter: LockTimeSetter
) {

    fun buildTransaction(toAddress: String, value: Long, feeRate: Int, senderPay: Boolean, sortType: TransactionDataSortType, pluginData: Map<Byte, IPluginData>
                         , unlockedHeight: Long?  /* UPDATE FOR SAFE */
                         , reverseHex: String?  /* UPDATE FOR SAFE */
    ): FullTransaction {

        val mutableTransaction = MutableTransaction()
        if ( unlockedHeight != null ) {
            mutableTransaction.unlockedHeight = unlockedHeight;
        }
        if (reverseHex != null) {
            mutableTransaction.reverseHex = reverseHex
        }
        recipientSetter.setRecipient(mutableTransaction, toAddress, value, pluginData, false)
        inputSetter.setInputs(mutableTransaction, feeRate, senderPay, sortType)
        lockTimeSetter.setLockTime(mutableTransaction)

        outputSetter.setOutputs(mutableTransaction, sortType)
        signer.sign(mutableTransaction)

        return mutableTransaction.build()
    }

    fun buildTransaction(unspentOutput: UnspentOutput, toAddress: String, feeRate: Int, sortType: TransactionDataSortType
                         , unlockedHeight:Long? /* UPDATE FOR SAFE */
                         , reverseHex: String?  /* UPDATE FOR SAFE */
    ): FullTransaction {

        val mutableTransaction = MutableTransaction(false)
        if ( unlockedHeight != null ){
            mutableTransaction.unlockedHeight = unlockedHeight;
        }
        if (reverseHex != null) {
            mutableTransaction.reverseHex = reverseHex
        }
        recipientSetter.setRecipient(mutableTransaction, toAddress, unspentOutput.output.value, mapOf(), false)
        inputSetter.setInputs(mutableTransaction, unspentOutput, feeRate)
        lockTimeSetter.setLockTime(mutableTransaction)

        outputSetter.setOutputs(mutableTransaction, sortType)
        signer.sign(mutableTransaction)

        return mutableTransaction.build()
    }

    open class BuilderException : Exception() {
        class FeeMoreThanValue : BuilderException()
        class NotSupportedScriptType : BuilderException()
    }
}
