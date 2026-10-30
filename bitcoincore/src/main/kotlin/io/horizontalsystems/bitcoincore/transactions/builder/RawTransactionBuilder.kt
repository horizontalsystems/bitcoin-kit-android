package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.DustCalculator
import io.horizontalsystems.bitcoincore.core.IPublicKeyManager
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.managers.BloomFilterManager
import io.horizontalsystems.bitcoincore.managers.SendValueErrors
import io.horizontalsystems.bitcoincore.managers.UnspentOutputSelectorChain
import io.horizontalsystems.bitcoincore.models.RawOutput
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.models.UnsignedTransaction
import io.horizontalsystems.bitcoincore.serializers.ParsedRawTransaction
import io.horizontalsystems.bitcoincore.serializers.RawTransactionCodec
import io.horizontalsystems.bitcoincore.serializers.TransactionSerializer
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.InputToSign
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.bitcoincore.transactions.PendingTransactionProcessor
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

/**
 * Builds, signs and registers transactions whose outputs are given as raw
 * locking scripts, for callers that assemble a transaction in several steps
 * with another component in between (the MWEB kit hands the unsigned
 * transaction to its daemon, which rewrites the outputs, before the inputs
 * are signed here).
 */
class RawTransactionBuilder(
    private val storage: IStorage,
    private val unspentOutputSelector: UnspentOutputSelectorChain,
    private val publicKeyManager: IPublicKeyManager,
    private val addressConverter: IAddressConverter,
    private val changeScriptType: ScriptType,
    private val sizeCalculator: TransactionSizeCalculator,
    private val dustCalculator: DustCalculator,
    private val transactionSigner: TransactionSigner,
    private val pendingTransactionProcessor: PendingTransactionProcessor,
    private val bloomFilterManager: BloomFilterManager,
) {

    /**
     * Selects wallet outputs (largest first) that cover [outputs] plus the
     * fee at [feeRate] satoshi per virtual byte, and returns the unsigned
     * transaction paying [outputs] in order with a change output appended
     * when the remainder is above dust. [extraVirtualBytes] is added to the
     * size the fee is computed for, e.g. for data a later step appends.
     */
    @Throws(SendValueErrors::class)
    fun buildUnsignedTransaction(
        outputs: List<RawOutput>,
        feeRate: Int,
        extraVirtualBytes: Int,
        unspentOutputs: List<UnspentOutput>?,
        changeToFirstInput: Boolean,
        filters: UtxoFilters,
    ): UnsignedTransaction {
        if (outputs.isEmpty()) throw SendValueErrors.EmptyOutputs
        val target = outputs.sumOf { it.value }
        val candidates = (unspentOutputs ?: unspentOutputSelector.getAllSpendable(filters))
            .sortedByDescending { it.output.value }
        if (candidates.any { it.output.failedToSpend }) throw SendValueErrors.HasOutputFailedToSpend

        val outputsSize = outputs.sumOf { sizeCalculator.outputSize(it.script.size) }
        val changeSize = sizeCalculator.outputSize(changeScriptType)
        val dust = dustCalculator.dust(changeScriptType)

        val selected = ArrayList<UnspentOutput>()
        var sum = 0L
        for (utxo in candidates) {
            selected.add(utxo)
            sum += utxo.output.value
            val baseSize = sizeCalculator.transactionSize(selected.map { it.output }, emptyList<TransactionOutput>()) +
                    outputsSize + extraVirtualBytes
            val feeWithoutChange = baseSize * feeRate
            if (sum < target + feeWithoutChange) continue

            val feeWithChange = (baseSize + changeSize) * feeRate
            val change = sum - target - feeWithChange
            val txOutputs = outputs.map { TransactionOutput(it.value, 0, it.script) }.toMutableList()
            var changeIndex: Int? = null
            var changeValue: Long? = null
            var fee = sum - target
            if (change > dust) {
                val changeScript = changeScript(selected.first(), changeToFirstInput)
                txOutputs.add(TransactionOutput(change, 0, changeScript))
                changeIndex = txOutputs.size - 1
                changeValue = change
                fee = feeWithChange
            }
            txOutputs.forEachIndexed { index, output -> output.index = index }

            val inputs = selected.map {
                TransactionInput(it.output.transactionHash, it.output.index.toLong(), byteArrayOf(), 0xfffffffeL)
            }
            val parsed = ParsedRawTransaction(
                version = 2,
                inputs = inputs,
                outputs = txOutputs,
                extensionFlags = 0,
                extension = null,
                lockTime = storage.lastBlock()?.height?.toLong() ?: 0L,
            )
            return UnsignedTransaction(
                rawTransaction = RawTransactionCodec.serialize(parsed),
                inputs = selected.map { UnspentOutputInfo.fromUnspentOutput(it) },
                fee = fee,
                changeOutputIndex = changeIndex,
                changeValue = changeValue,
            )
        }
        throw SendValueErrors.InsufficientUnspentOutputs
    }

    /**
     * Signs every input of [rawTransaction] with the wallet keys of the
     * outputs in [inputs] and returns the signed serialization, keeping any
     * fork extension the transaction carries. Every input must spend one of
     * [inputs]: the segwit signature hash commits to all of them.
     */
    fun signTransaction(rawTransaction: ByteArray, inputs: List<UnspentOutputInfo>): ByteArray {
        val parsed = RawTransactionCodec.parse(rawTransaction)
        val available = storage.getUnspentOutputs()
        val mutable = MutableTransaction()
        mutable.transaction.version = parsed.version
        mutable.transaction.lockTime = parsed.lockTime
        mutable.outputs = parsed.outputs

        for (input in parsed.inputs) {
            val info = inputs.firstOrNull {
                it.transactionHash.contentEquals(input.previousOutputTxHash) && it.outputIndex.toLong() == input.previousOutputIndex
            } ?: throw IllegalArgumentException(
                "Input ${input.previousOutputTxHash.toHexString()}:${input.previousOutputIndex} is not a wallet output"
            )
            val unspent = available.firstOrNull {
                it.output.transactionHash.contentEquals(info.transactionHash) && it.output.index == info.outputIndex
            } ?: throw IllegalArgumentException(
                "Wallet output ${info.transactionHash.toHexString()}:${info.outputIndex} not found"
            )
            mutable.addInput(InputToSign(input, unspent.output, unspent.publicKey))
        }

        transactionSigner.sign(mutable)

        val signed = ParsedRawTransaction(
            version = parsed.version,
            inputs = mutable.inputsToSign.map { it.input },
            outputs = parsed.outputs,
            extensionFlags = parsed.extensionFlags,
            extension = parsed.extension,
            lockTime = parsed.lockTime,
        )
        return RawTransactionCodec.serialize(signed)
    }

    /**
     * Records [signedTransaction], which has been broadcast by other means,
     * as an outgoing wallet transaction so its inputs are spent and its
     * change is available at once. A fork extension is dropped: the wallet
     * stores and matches the canonical transaction.
     */
    fun registerPendingTransaction(signedTransaction: ByteArray): FullTransaction {
        val parsed = RawTransactionCodec.parse(signedTransaction)
        val canonical = RawTransactionCodec.serialize(parsed, withWitness = true, withExtension = false)
        val fullTransaction = TransactionSerializer.deserialize(BitcoinInputMarkable(canonical))
        fullTransaction.header.isMine = true
        fullTransaction.header.isOutgoing = true
        fullTransaction.header.status = Transaction.Status.RELAYED
        fullTransaction.header.rawTransaction = canonical.toHexString()
        try {
            pendingTransactionProcessor.processCreated(fullTransaction)
        } catch (e: BloomFilterManager.BloomFilterExpired) {
            bloomFilterManager.regenerateBloomFilter()
        }
        return fullTransaction
    }

    private fun changeScript(firstInput: UnspentOutput, changeToFirstInput: Boolean): ByteArray {
        val address = if (changeToFirstInput) {
            addressConverter.convert(firstInput.publicKey, firstInput.output.scriptType)
        } else {
            addressConverter.convert(publicKeyManager.changePublicKey(), changeScriptType)
        }
        return address.lockingScript
    }
}
