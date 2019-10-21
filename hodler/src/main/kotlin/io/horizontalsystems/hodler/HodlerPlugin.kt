package io.horizontalsystems.hodler

import io.horizontalsystems.bitcoincore.blocks.BlockMedianTimeHelper
import io.horizontalsystems.bitcoincore.core.IPlugin
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.scripts.*
import io.horizontalsystems.bitcoincore.utils.IAddressConverter
import io.horizontalsystems.bitcoincore.utils.Utils

class HodlerPlugin : IPlugin {

    companion object {
        const val id = OP_1.toByte()
    }

    enum class LockTimeInterval(val value: Int) {
        hour(7),
        month(5063),     //  30 * 24 * 60 * 60 / 512
        halfYear(30881), // 183 * 24 * 60 * 60 / 512
        year(61593);     // 365 * 24 * 60 * 60 / 512

        fun to2BytesLE(): ByteArray {
            // need to write to extra data output as 2 bytes
            return Utils.intToByteArray(value).reversedArray().copyOfRange(0, 2)
        }

        companion object {
            fun fromValue(value: Int): LockTimeInterval? {
                return values().find {
                    it.value == value
                }
            }

            fun from2BytesLE(bytes: ByteArray): LockTimeInterval? {
                if (bytes.size != 2) return null

                return fromValue(Utils.byteArrayToUInt16LE(bytes))
            }
        }
    }

    private val sequenceTimeSecondsGranularity = 512
    private val relativeLockTimeLockMask = 0x400000 // (1 << 22)

    override val id = HodlerPlugin.id

    override fun processOutputs(mutableTransaction: MutableTransaction, pluginData: Map<String, Any>, addressConverter: IAddressConverter) {
//        val lockTimeInterval = LockTimeInterval.hour
        val lockTimeInterval = checkNotNull(pluginData["lockTimeInterval"] as? LockTimeInterval)

        check(mutableTransaction.recipientAddress.scriptType == ScriptType.P2PKH) {
            "Locking transaction is available only for PKH addresses"
        }

        val pubkeyHash = mutableTransaction.recipientAddress.hash
        val redeemScriptHash = Utils.sha256Hash160(redeemScript(lockTimeInterval, pubkeyHash))
        val newAddress = addressConverter.convert(redeemScriptHash, ScriptType.P2SH)

        mutableTransaction.recipientAddress = newAddress
        mutableTransaction.addPluginData(id, OpCodes.push(lockTimeInterval.to2BytesLE()) + OpCodes.push(pubkeyHash))
    }

    override fun processTransactionWithNullData(transaction: FullTransaction, nullDataChunks: Iterator<Script.Chunk>, storage: IStorage, addressConverter: IAddressConverter) {
        val lockTimeIntervalData = checkNotNull(nullDataChunks.next().data)
        val pubkeyHash = checkNotNull(nullDataChunks.next().data)

        val lockTimeInterval = checkNotNull(LockTimeInterval.from2BytesLE(lockTimeIntervalData))

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

    override fun isSpendable(unspentOutput: UnspentOutput, blockMedianTimeHelper: BlockMedianTimeHelper): Boolean {
        val lastBlockMedianTimePast = blockMedianTimeHelper.medianTimePast ?: return false
        return inputLockTime(unspentOutput) < lastBlockMedianTimePast
    }

    override fun getInputSequence(output: TransactionOutput): Long {
        return sequence(lockTimeIntervalFrom(output)).toLong()
    }

    override fun parsePluginData(output: TransactionOutput): Map<String, Any> {
        val hodlerData = HodlerData.parse(output.pluginData)

        return mapOf("lockTimeInterval" to hodlerData.lockTimeInterval, "address" to hodlerData.addressString)
    }

    override fun keysForApiRestore(publicKey: PublicKey, addressConverter: IAddressConverter): List<String> {
        return LockTimeInterval.values().map { lockTimeInterval ->
            val redeemScript = redeemScript(lockTimeInterval, publicKey.publicKeyHash)
            val redeemScriptHash = Utils.sha256Hash160(redeemScript)

            addressConverter.convert(redeemScriptHash, ScriptType.P2SH).string
        }
    }

    private fun redeemScript(lockTimeInterval: LockTimeInterval, pubkeyHash: ByteArray): ByteArray {
        val sequenceData = Utils.intToByteArray(sequence(lockTimeInterval)).reversedArray().copyOfRange(0, 3)
        return OpCodes.push(sequenceData) + byteArrayOf(OP_CHECKSEQUENCEVERIFY.toByte(), OP_DROP.toByte()) + OpCodes.p2pkhStart + OpCodes.push(pubkeyHash) + OpCodes.p2pkhEnd
    }

    private fun sequence(lockTimeInterval: LockTimeInterval): Int {
        return (relativeLockTimeLockMask or lockTimeInterval.value)
    }

    private fun lockTimeIntervalFrom(output: TransactionOutput): LockTimeInterval {
        val pluginData = checkNotNull(output.pluginData)

        return HodlerData.parse(pluginData).lockTimeInterval
    }

    private fun inputLockTime(unspentOutput: UnspentOutput): Long {
        // Use (an approximate medianTimePast of a block in which given transaction is included) PLUS ~1 hour.
        // This is not an accurate medianTimePast, it is always a timestamp nearly 7 blocks ahead.
        // But this is quite enough in our case since we're setting relative time-locks for at least 1 month
        val previousOutputMedianTime = unspentOutput.transaction.timestamp

        val lockTimeInterval = lockTimeIntervalFrom(unspentOutput.output)

        return previousOutputMedianTime + lockTimeInterval.value * sequenceTimeSecondsGranularity
    }
}
