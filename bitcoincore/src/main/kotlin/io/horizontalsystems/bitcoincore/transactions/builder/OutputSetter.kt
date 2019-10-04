package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptBuilder
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

class OutputSetter(private val scriptBuilder: ScriptBuilder, private val addressConverter: IAddressConverter) {

    fun setOutputs(transaction: MutableTransaction, addressStr: String, value: Long) {
        val address = addressConverter.convert(addressStr)
        val transactionOutput = TransactionOutput(value, 0, scriptBuilder.lockingScript(address), address.scriptType, address.string, address.hash)

        transaction.paymentOutput = transactionOutput
    }

}
