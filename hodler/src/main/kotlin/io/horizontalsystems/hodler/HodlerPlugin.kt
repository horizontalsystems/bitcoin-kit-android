package io.horizontalsystems.hodler

import io.horizontalsystems.bitcoincore.core.IPlugin
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.scripts.*
import io.horizontalsystems.bitcoincore.utils.IAddressConverter
import io.horizontalsystems.bitcoincore.utils.Utils

class HodlerPlugin : IPlugin {

    override fun processOutputs(mutableTransaction: MutableTransaction, extraData: Map<String, Map<String, Any>>, scriptBuilder: ScriptBuilder, addressConverter: IAddressConverter) {
        val lockedUntilTimestamp = extraData["hodler"]?.get("locked_until") as? Int ?: return

        val paymentOutput = mutableTransaction.paymentOutput

        if (paymentOutput.scriptType != ScriptType.P2PKH) {
            throw Exception("Locking transaction is available only for PKH addresses")
        }

        val cltv = OpCodes.push(Utils.intToByteArray(lockedUntilTimestamp).reversedArray()) + byteArrayOf(OP_CHECKLOCKTIMEVERIFY.toByte(), OP_DROP.toByte())
        val newLockingScript = cltv + paymentOutput.lockingScript
        val newLockingScriptHash = Utils.sha256Hash160(newLockingScript)

        val newAddress = addressConverter.convert(newLockingScriptHash, ScriptType.P2SH)

        val newPaymentOutput = TransactionOutput(paymentOutput.value, 0, scriptBuilder.lockingScript(newAddress), newAddress.scriptType, newAddress.string, newAddress.hash)

        val data = byteArrayOf(OP_RETURN.toByte()) + OpCodes.push(Utils.intToByteArray(lockedUntilTimestamp.toInt()).reversedArray()) + OpCodes.push(paymentOutput.keyHash!!)
        val dataOutput = TransactionOutput(0, 2, data)

        mutableTransaction.paymentOutput = newPaymentOutput
        mutableTransaction.dataOutput = dataOutput
    }

}
