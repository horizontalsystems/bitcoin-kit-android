package io.horizontalsystems.hodler

import io.horizontalsystems.bitcoincore.blocks.BlockMedianTimeHelper
import io.horizontalsystems.bitcoincore.core.IPlugin
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.scripts.*
import io.horizontalsystems.bitcoincore.utils.IAddressConverter
import io.horizontalsystems.bitcoincore.utils.Utils

class HodlerPlugin : IPlugin {

    enum class LockTimeInterval(val value: Int) {
        hour(7),
        month(5063),     //  30 * 24 * 60 * 60 / 512
        halfYear(30881), // 183 * 24 * 60 * 60 / 512
        year(61593);     // 365 * 24 * 60 * 60 / 512

        companion object {
            fun fromValue(value: Int): LockTimeInterval? {
                return values().find {
                    it.value == value
                }
            }
        }
    }

    private val sequenceTimeSecondsGranularity = 512
    private val relativeLockTimeLockMask = 0x400000 // (1 << 22)

    override val id = OP_1

    override fun processOutputs(mutableTransaction: MutableTransaction, extraData: Map<String, Map<String, Any>>, addressConverter: IAddressConverter) {
//        val lockTimeInterval = LockTimeInterval.hour
        val lockTimeInterval = extraData["hodler"]?.get("lockTimeInterval") as? LockTimeInterval ?: return

        if (mutableTransaction.recipientAddress.scriptType != ScriptType.P2PKH) {
            throw Exception("Locking transaction is available only for PKH addresses")
        }

        val pubkeyHash = mutableTransaction.recipientAddress.hash

        val redeemScript = redeemScript(lockTimeInterval, pubkeyHash)
        val redeemScriptHash = Utils.sha256Hash160(redeemScript)

        val newAddress = addressConverter.convert(redeemScriptHash, ScriptType.P2SH)

        // write time period to extra data as 2 bytes
        val periodIn2Bytes = Utils.intToByteArray(lockTimeInterval.value).reversedArray().copyOfRange(0, 2)

        mutableTransaction.recipientAddress = newAddress
        mutableTransaction.addExtraData(id, OpCodes.push(periodIn2Bytes) + OpCodes.push(pubkeyHash))
    }

    override fun processTransactionWithNullData(transaction: FullTransaction, nullDataChunks: Iterator<Script.Chunk>, storage: IStorage, addressConverter: IAddressConverter) {
        val lockTimeIntervalData = nullDataChunks.next().data
                ?: throw Exception("invalidHodlerData")
        val pubkeyHash = nullDataChunks.next().data ?: throw Exception("invalidHodlerData")

        val lockTimeInterval = lockTimeIntervalFrom(lockTimeIntervalData)
                ?: throw Exception("invalidHodlerData")

        val redeemScript = redeemScript(lockTimeInterval, pubkeyHash)
        val redeemScriptHash = Utils.sha256Hash160(redeemScript)

        transaction.outputs.find {
            it.keyHash?.contentEquals(redeemScriptHash) ?: false
        }?.let { output ->
            val addressString = addressConverter.convert(pubkeyHash, ScriptType.P2PKH).string

            output.pluginId = id
            output.pluginData = HodlerData(lockTimeInterval, addressString).toString()

            storage.getPublicKeyByKeyOrKeyHash(pubkeyHash)?.let { pubkey ->
                output.redeemScript = redeemScript
                output.publicKeyPath = pubkey.path
                transaction.header.isMine = true
            }
        }
    }

    override fun isSpendable(output: TransactionOutput, blockMedianTimeHelper: BlockMedianTimeHelper): Boolean {
        val lastBlockMedianTimePast = blockMedianTimeHelper.medianTimePast ?: return false
        return inputLockTime(output, blockMedianTimeHelper) < lastBlockMedianTimePast
    }

    override fun getInputSequence(output: TransactionOutput): Long {
        return sequence(lockTimeIntervalFrom(output)).toLong()
    }

    override fun parsePluginData(output: TransactionOutput): Map<String, Any> {
        val hodlerData = HodlerData.parse(output.pluginData)

        return mapOf("lockTimeInterval" to hodlerData.lockTimeInterval, "address" to hodlerData.addressString)
    }

    private fun redeemScript(lockTimeInterval: LockTimeInterval, pubkeyHash: ByteArray): ByteArray {
        val sequenceData = Utils.intToByteArray(sequence(lockTimeInterval)).reversedArray().copyOfRange(0, 3)
        return OpCodes.push(sequenceData) + byteArrayOf(OP_CHECKSEQUENCEVERIFY.toByte(), OP_DROP.toByte()) + OpCodes.p2pkhStart + OpCodes.push(pubkeyHash) + OpCodes.p2pkhEnd
    }

    private fun sequence(lockTimeInterval: LockTimeInterval) : Int {
        return (relativeLockTimeLockMask or lockTimeInterval.value)
    }

    private fun lockTimeIntervalFrom(lockTimeIntervalData: ByteArray): LockTimeInterval? {
        if (lockTimeIntervalData.size != 2) return null

        return LockTimeInterval.fromValue(Utils.byteArrayToUInt16LE(lockTimeIntervalData))
    }

    private fun lockTimeIntervalFrom(output: TransactionOutput): LockTimeInterval {
        val pluginData = checkNotNull(output.pluginData) { "invalidHodlerData" }

        return HodlerData.parse(pluginData).lockTimeInterval
    }

    private fun inputLockTime(output: TransactionOutput, blockMedianTimeHelper: BlockMedianTimeHelper): Long {
        val previousOutputMedianTime = checkNotNull(blockMedianTimeHelper.medianTimePast(output.transactionHash)) {
            "HodlerPluginError.invalidHodlerData"
        }

        val lockTimeInterval = lockTimeIntervalFrom(output)

        return previousOutputMedianTime + lockTimeInterval.value * sequenceTimeSecondsGranularity
    }
}
