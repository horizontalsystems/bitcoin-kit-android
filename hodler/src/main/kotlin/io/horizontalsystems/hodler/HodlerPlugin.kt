package io.horizontalsystems.hodler

import io.horizontalsystems.bitcoincore.core.IPlugin
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.io.BitcoinInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.scripts.*
import io.horizontalsystems.bitcoincore.utils.IAddressConverter
import io.horizontalsystems.bitcoincore.utils.Utils

class HodlerPlugin : IPlugin {
    override val id = OP_1

    override fun processOutputs(mutableTransaction: MutableTransaction, extraData: Map<String, Map<String, Any>>, addressConverter: IAddressConverter) {
        val lockedUntilTimestamp = extraData["hodler"]?.get("locked_until") as? Int ?: return

        if (mutableTransaction.recipientAddress.scriptType != ScriptType.P2PKH) {
            throw Exception("Locking transaction is available only for PKH addresses")
        }

        val lockedUntil = Utils.intToByteArray(lockedUntilTimestamp).reversedArray()
        val pubkeyHash = mutableTransaction.recipientAddress.hash

        val newLockingScript = redeemScript(lockedUntil, pubkeyHash)
        val newLockingScriptHash = Utils.sha256Hash160(newLockingScript)

        val newAddress = addressConverter.convert(newLockingScriptHash, ScriptType.P2SH)

        mutableTransaction.recipientAddress = newAddress
        mutableTransaction.addExtraData(id, OpCodes.push(lockedUntil) + OpCodes.push(pubkeyHash))
    }

    override fun processTransactionWithNullData(transaction: FullTransaction, nullDataChunks: Iterator<Script.Chunk>, storage: IStorage, addressConverter: IAddressConverter) {
        val lockedUntil = nullDataChunks.next().data
        val pubkeyHash = nullDataChunks.next().data

        if (lockedUntil != null && pubkeyHash != null) {
            val redeemScript = redeemScript(lockedUntil, pubkeyHash)
            val redeemScriptHash = Utils.sha256Hash160(redeemScript)

            transaction.outputs.find { it.keyHash?.contentEquals(redeemScriptHash) ?: false }?.let {
                val lockedUntilTimestamp = BitcoinInput(lockedUntil).readUnsignedInt()
                val addressString = addressConverter.convert(pubkeyHash, ScriptType.P2PKH).string

                it.pluginId = id
                it.pluginData = HodlerData(lockedUntilTimestamp, addressString).toString()

                storage.getPublicKeyByKeyOrKeyHash(pubkeyHash)?.let { pubkey ->
                    it.redeemScript = redeemScript
                    it.publicKeyPath = pubkey.path
                    transaction.header.isMine = true
                }
            }
        }
    }

    override fun isSpendable(output: TransactionOutput, blockMedianTime: Long): Boolean {
        return getTransactionLockTime(output) < blockMedianTime
    }

    override fun getTransactionLockTime(output: TransactionOutput): Long {
        return HodlerData.parse(output.pluginData).lockedUntilTimestamp
    }

    override fun parsePluginData(output: TransactionOutput): Map<String, Any> {
        val hodlerData = HodlerData.parse(output.pluginData)

        return mapOf("locked_until" to hodlerData.lockedUntilTimestamp, "address" to hodlerData.addressString)
    }

    private fun redeemScript(lockedUntil: ByteArray, pubkeyHash: ByteArray): ByteArray {
        return OpCodes.push(lockedUntil) + byteArrayOf(OP_CHECKLOCKTIMEVERIFY.toByte(), OP_DROP.toByte()) + OpCodes.p2pkhStart + OpCodes.push(pubkeyHash) + OpCodes.p2pkhEnd
    }
}
