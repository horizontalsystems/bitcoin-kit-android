package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.managers.IUnspentOutputSelector
import io.horizontalsystems.bitcoincore.managers.PublicKeyManager
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.InputToSign
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptBuilder
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

class InputSetter(
        private val unspentOutputSelector: IUnspentOutputSelector,
        private val publicKeyManager: PublicKeyManager,
        private val addressConverter: IAddressConverter,
        private val scriptBuilder: ScriptBuilder,
        private val changeScriptType: Int
) {
    fun setInputs(mutableTransaction: MutableTransaction, feeRate: Int, senderPay: Boolean) {
        val value = mutableTransaction.paymentOutput.value

        val unspentOutputInfo = unspentOutputSelector.select(value, feeRate, mutableTransaction.paymentOutput.scriptType, changeScriptType, senderPay)

        val unspentOutputs = unspentOutputInfo.outputs
        for (unspentOutput in unspentOutputs) {
            val previousOutput = unspentOutput.output
            val transactionInput = TransactionInput(previousOutput.transactionHash, previousOutput.index.toLong())

            if (unspentOutput.output.scriptType == ScriptType.P2WPKH) {
                unspentOutput.output.keyHash = unspentOutput.output.keyHash?.drop(2)?.toByteArray()
            }

            val inputToSign = InputToSign(transactionInput, previousOutput, unspentOutput.publicKey)
            mutableTransaction.addInput(inputToSign)
        }

        //  calculate fee and add change output if needed
        val fee = unspentOutputInfo.fee

        val receivedValue = if (senderPay) value else value - fee
        mutableTransaction.paymentOutput.value = receivedValue

        if (unspentOutputInfo.addChangeOutput) {
            val changePubKey = publicKeyManager.changePublicKey()
            val changeAddress = addressConverter.convert(changePubKey, changeScriptType)

            val sentValue = if (senderPay) value + fee else value
            val totalValue = unspentOutputs.fold(0L) { sum, unspent -> sum + unspent.output.value }
            val changeOutput = TransactionOutput(totalValue - sentValue, 1, scriptBuilder.lockingScript(changeAddress), changeAddress.scriptType, changeAddress.string, changeAddress.hash)

            mutableTransaction.changeOutput = changeOutput
        }
    }

}
