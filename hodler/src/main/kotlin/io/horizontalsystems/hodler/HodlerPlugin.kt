package io.horizontalsystems.hodler

import io.horizontalsystems.bitcoincore.core.IPlugin
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
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

        val data = byteArrayOf(OP_RETURN.toByte()) + byteArrayOf(OP_1.toByte()) + OpCodes.push(Utils.intToByteArray(lockedUntilTimestamp).reversedArray()) + OpCodes.push(paymentOutput.keyHash!!)
        val dataOutput = TransactionOutput(0, 2, data, ScriptType.NULL_DATA)

        mutableTransaction.paymentOutput = newPaymentOutput
        mutableTransaction.dataOutput = dataOutput
    }

    override fun processTransactionWithNullData(transaction: FullTransaction, nullDataOutput: TransactionOutput, storage: IStorage) {
        val data = nullDataOutput.lockingScript

        val script = Script(data)

        script.chunks.forEachIndexed { index, chunk ->
            if (chunk.opcode == OP_1) {
                val lockedUntil = script.chunks[index + 1].data
                val pubkeyHash = script.chunks[index + 2].data
                val pubkey = pubkeyHash?.let { storage.getPublicKeyByKeyOrKeyHash(pubkeyHash) }

                if (lockedUntil != null && pubkeyHash != null && pubkey != null) {

                    val redeemScript = OpCodes.push(lockedUntil) + byteArrayOf(OP_CHECKLOCKTIMEVERIFY.toByte(), OP_DROP.toByte()) +
                            OpCodes.p2pkhStart + OpCodes.push(pubkeyHash) + OpCodes.p2pkhEnd

                    val redeemScriptHash = Utils.sha256Hash160(redeemScript)

                    transaction.outputs.find { it.keyHash?.contentEquals(redeemScriptHash) ?: false }?.let {

                        it.redeemScript = redeemScript
                        it.publicKeyPath = pubkey.path
//                        val lockedUntilTimestamp = Utils.readUint32FromStream(ByteArrayInputStream(lockedUntil))
//                        it.extraData = mapOf("hodler" to mapOf("locked_until" to lockedUntilTimestamp))

                        transaction.header.isMine = true

                        return@forEachIndexed
                    }
                }
            }
        }
    }
}
