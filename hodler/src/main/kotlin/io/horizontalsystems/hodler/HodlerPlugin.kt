package io.horizontalsystems.hodler

import io.horizontalsystems.bitcoincore.blocks.BlockMedianTimeHelper
import io.horizontalsystems.bitcoincore.core.IPlugin
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.core.IPluginOutputData
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.scripts.*
import io.horizontalsystems.bitcoincore.utils.IAddressConverter
import io.horizontalsystems.bitcoincore.utils.Utils

class HodlerPlugin(
        private val addressConverter: IAddressConverter,
        private val storage: IStorage,
        private val blockMedianTimeHelper: BlockMedianTimeHelper
) : IPlugin {

    companion object {
        const val id = OP_1.toByte()
        private const val limit = 50_000_000L
    }

    override val id = HodlerPlugin.id

    override fun processOutputs(mutableTransaction: MutableTransaction, pluginData: IPluginData, skipChecking: Boolean) {
        val lockTimeInterval = checkNotNull((pluginData as? HodlerData)?.lockTimeInterval)

        if (!skipChecking) {
            check(mutableTransaction.recipientAddress.scriptType == ScriptType.P2PKH) {
                "Locking transaction is available only for PKH addresses"
            }

            check(mutableTransaction.recipientValue <= limit) {
                "The maximum amount allowed for locking is $limit"
            }
        }

        val pubkeyHash = mutableTransaction.recipientAddress.hash
        val redeemScriptHash = Utils.sha256Hash160(redeemScript(lockTimeInterval, pubkeyHash))
        val newAddress = addressConverter.convert(redeemScriptHash, ScriptType.P2SH)

        mutableTransaction.recipientAddress = newAddress
        mutableTransaction.addPluginData(id, OpCodes.push(lockTimeInterval.valueAs2BytesLE) + OpCodes.push(pubkeyHash))
    }

    override fun processTransactionWithNullData(transaction: FullTransaction, nullDataChunks: Iterator<Script.Chunk>) {
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
            output.pluginData = HodlerOutputData(lockTimeInterval, addressString).serialize()

            storage.getPublicKeyByKeyOrKeyHash(pubkeyHash)?.let { pubkey ->
                output.redeemScript = redeemScript
                output.setPublicKey(pubkey)
                transaction.header.isMine = true
            }
        }
    }

    override fun isSpendable(unspentOutput: UnspentOutput): Boolean {
        val lastBlockMedianTimePast = blockMedianTimeHelper.medianTimePast ?: return false
        return inputLockTime(unspentOutput) < lastBlockMedianTimePast
    }

    override fun getInputSequence(output: TransactionOutput): Long {
        return lockTimeIntervalFrom(output).sequenceNumber.toLong()
    }

    override fun parsePluginData(output: TransactionOutput, txTimestamp: Long): IPluginOutputData {
        val hodlerData = HodlerOutputData.parse(output.pluginData)

        // When checking if utxo is spendable we use the best block median time.
        // The median time is 6 blocks earlier which is approximately equal to 1 hour.
        // Here we add 1 hour to show the time when this UTXO will be spendable
        hodlerData.approxUnlockTime = hodlerData.lockTimeInterval.valueInSeconds + txTimestamp + 3600

        return hodlerData
    }

    override fun keysForApiRestore(publicKey: PublicKey): List<String> {
        return LockTimeInterval.values().map { lockTimeInterval ->
            val redeemScript = redeemScript(lockTimeInterval, publicKey.publicKeyHash)
            val redeemScriptHash = Utils.sha256Hash160(redeemScript)

            addressConverter.convert(redeemScriptHash, ScriptType.P2SH).string
        }
    }

    // The maximum lock amount is 0.5 bitcoins
    override fun maximumSpendLimit(): Long? {
        return limit
    }

    override fun validateAddress(address: Address) {
        if (address.scriptType != ScriptType.P2PKH) {
            throw UnsupportedAddressType()
        }
    }

    private fun redeemScript(lockTimeInterval: LockTimeInterval, pubkeyHash: ByteArray): ByteArray {
        return OpCodes.push(lockTimeInterval.sequenceNumberAs3BytesLE) + byteArrayOf(OP_CHECKSEQUENCEVERIFY.toByte(), OP_DROP.toByte()) + OpCodes.p2pkhStart + OpCodes.push(pubkeyHash) + OpCodes.p2pkhEnd
    }

    private fun lockTimeIntervalFrom(output: TransactionOutput): LockTimeInterval {
        val pluginData = checkNotNull(output.pluginData)

        return HodlerOutputData.parse(pluginData).lockTimeInterval
    }

    private fun inputLockTime(unspentOutput: UnspentOutput): Long {
        // Use (an approximate medianTimePast of a block in which given transaction is included) PLUS ~1 hour.
        // This is not an accurate medianTimePast, it is always a timestamp nearly 7 blocks ahead.
        // But this is quite enough in our case since we're setting relative time-locks for at least 1 month
        val previousOutputMedianTime = unspentOutput.transaction.timestamp

        val lockTimeInterval = lockTimeIntervalFrom(unspentOutput.output)

        return previousOutputMedianTime + lockTimeInterval.valueInSeconds
    }

    class UnsupportedAddressType : Exception()
}
