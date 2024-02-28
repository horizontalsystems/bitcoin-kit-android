package io.horizontalsystems.bitcoincore.rbf

import io.horizontalsystems.bitcoincore.DustCalculator
import io.horizontalsystems.bitcoincore.core.IPublicKeyManager
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.core.PluginManager
import io.horizontalsystems.bitcoincore.extensions.toReversedByteArray
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.managers.UnspentOutputProvider
import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.models.TransactionType
import io.horizontalsystems.bitcoincore.models.rbfEnabled
import io.horizontalsystems.bitcoincore.storage.FullTransactionInfo
import io.horizontalsystems.bitcoincore.storage.InputToSign
import io.horizontalsystems.bitcoincore.storage.InputWithPreviousOutput
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.extractors.TransactionMetadataExtractor
import io.horizontalsystems.bitcoincore.utils.ShuffleSorter

class ReplacementTransactionBuilder(
    private val storage: IStorage,
    private val sizeCalculator: TransactionSizeCalculator,
    private val dustCalculator: DustCalculator,
    private val metadataExtractor: TransactionMetadataExtractor,
    private val pluginManager: PluginManager,
    private val unspentOutputProvider: UnspentOutputProvider,
    private val publicKeyManager: IPublicKeyManager,
) {

    private fun replacementTransaction(
        minFee: Long,
        minFeeRate: Int,
        utxo: List<TransactionOutput>,
        fixedOutputs: List<TransactionOutput>,
        outputs: List<TransactionOutput>
    ): Pair<List<TransactionOutput>, /*Fee*/Long>? {

        var minFee = minFee
        val size = sizeCalculator.transactionSize(previousOutputs = utxo, outputs = fixedOutputs + outputs)

        val inputsValue = utxo.sumOf { it.value }
        val outputsValue = (fixedOutputs + outputs).sumOf { it.value }
        val fee = inputsValue - outputsValue
        val feeRate = fee / size

        if (feeRate < minFeeRate) {
            minFee = minFeeRate * size
        }

        if (fee >= minFee) {
            return Pair(outputs, fee)
        }

        if (outputs.isEmpty()) {
            return null
        }

        val output = TransactionOutput(outputs.first())
        output.value = output.value - (minFee - fee)

        if (output.value > dustCalculator.dust(output.scriptType)) {
            return Pair(listOf(output) + outputs.drop(1), fee)
        }

        return null
    }

    private fun incrementedSequence(input: TransactionInput): Long {
        return input.sequence + 1 // TODO: increment locked inputs sequence
    }

    private fun inputToSign(previousOutput: TransactionOutput, publicKey: PublicKey, sequence: Long): InputToSign {
        val transactionInput = TransactionInput(previousOutput.transactionHash, previousOutput.index.toLong(), sequence = sequence)

        return InputToSign(transactionInput, previousOutput, publicKey)
    }

    private fun setInputs(
        mutableTransaction: MutableTransaction,
        originalInputs: List<InputWithPreviousOutput>,
        additionalInputs: List<UnspentOutput>
    ) {
        additionalInputs.map { utxo ->
            mutableTransaction.addInput(inputToSign(previousOutput = utxo.output, publicKey = utxo.publicKey, sequence = 0x0))
        }

        pluginManager.processInputs(mutableTransaction)

        originalInputs.map { inputWithPreviousOutput ->
            val previousOutput = inputWithPreviousOutput.previousOutput ?: throw BuildError.InvalidTransaction
            val publicKey = previousOutput.publicKeyPath?.let { publicKeyManager.getPublicKeyByPath(it) } ?: throw BuildError.InvalidTransaction
            mutableTransaction.addInput(inputToSign(previousOutput = previousOutput, publicKey, incrementedSequence(inputWithPreviousOutput.input)))
        }
    }

    private fun setOutputs(mutableTransaction: MutableTransaction, outputs: List<TransactionOutput>) {
        val sorted = ShuffleSorter().sortOutputs(outputs)
        sorted.forEachIndexed { index, transactionOutput ->
            transactionOutput.index = index
        }

        mutableTransaction.outputs = sorted
    }

    private fun speedUpReplacement(
        originalFullInfo: FullTransactionInfo,
        minFee: Long,
        originalFeeRate: Int,
        fixedUtxo: List<TransactionOutput>
    ): MutableTransaction? {
        // If an output has a pluginId, it most probably has a time-locked value and it shouldn't be altered.
        val fixedOutputs = originalFullInfo.outputs.filter { it.publicKeyPath == null || it.pluginId != null }
        val myOutputs = originalFullInfo.outputs.filter { it.publicKeyPath != null && it.pluginId == null }
        val myChangeOutputs = myOutputs.filter { it.changeOutput }.sortedBy { it.value }
        val myExternalOutputs = myOutputs.filter { !it.changeOutput }.sortedBy { it.value }

        val sortedOutputs = myChangeOutputs + myExternalOutputs
        val unusedUtxo = unspentOutputProvider.getConfirmedSpendableUtxo().sortedBy { it.output.value }
        var optimalReplacement: Triple</*inputs*/ List<UnspentOutput>, /*outputs*/ List<TransactionOutput>, /*fee*/ Long>? = null

        var utxoCount = 0
        do {
            val utxo = unusedUtxo.take(utxoCount)
            var outputsCount = sortedOutputs.size

            do {
                val outputs = sortedOutputs.takeLast(outputsCount)

                replacementTransaction(
                    minFee = minFee,
                    minFeeRate = originalFeeRate,
                    utxo = fixedUtxo + utxo.map { it.output },
                    fixedOutputs = fixedOutputs,
                    outputs = outputs
                )?.let { (outputs, fee) ->
                    optimalReplacement.let { _optimalReplacement ->
                        if (_optimalReplacement != null) {
                            if (_optimalReplacement.third > fee) {
                                optimalReplacement = Triple(utxo, outputs, fee)
                            }
                        } else {
                            optimalReplacement = Triple(utxo, outputs, fee)
                        }
                    }
                }

                outputsCount--
            } while (outputsCount >= 0)

            utxoCount++
        } while (utxoCount <= unusedUtxo.size)

        return optimalReplacement?.let { (inputs, outputs, _) ->
            val mutableTransaction = MutableTransaction()
            setInputs(
                mutableTransaction = mutableTransaction,
                originalInputs = originalFullInfo.inputs,
                additionalInputs = inputs
            )
            setOutputs(
                mutableTransaction = mutableTransaction,
                outputs = fixedOutputs + outputs
            )
            mutableTransaction
        }
    }

    private fun cancelReplacement(
        originalFullInfo: FullTransactionInfo,
        minFee: Long,
        originalFee: Long,
        originalFeeRate: Int,
        fixedUtxo: List<TransactionOutput>,
        changeAddress: Address
    ): MutableTransaction? {
        val unusedUtxo = unspentOutputProvider.getConfirmedSpendableUtxo().sortedBy { it.output.value }
        val originalInputsValue = fixedUtxo.sumOf { it.value }

        var optimalReplacement: Triple</*inputs*/ List<UnspentOutput>, /*outputs*/ List<TransactionOutput>, /*fee*/ Long>? = null

        var utxoCount = 0
        val outputs = listOf(
            TransactionOutput(
                value = originalInputsValue - originalFee,
                index = 0,
                script = changeAddress.lockingScript,
                type = changeAddress.scriptType,
                address = changeAddress.stringValue,
                lockingScriptPayload = changeAddress.lockingScriptPayload
            )
        )
        do {
            val utxo = unusedUtxo.take(utxoCount)

            replacementTransaction(
                minFee = minFee,
                minFeeRate = originalFeeRate,
                utxo = fixedUtxo + utxo.map { it.output },
                fixedOutputs = listOf(),
                outputs = outputs
            )?.let { (outputs, fee) ->
                optimalReplacement.let { _optimalReplacement ->
                    if (_optimalReplacement != null) {
                        if (_optimalReplacement.third > fee) {
                            optimalReplacement = Triple(utxo, outputs, fee)
                        }
                    } else {
                        optimalReplacement = Triple(utxo, outputs, fee)
                    }
                }
            }

            utxoCount++
        } while (utxoCount <= unusedUtxo.size)


        return optimalReplacement?.let { (inputs, outputs, _) ->
            val mutableTransaction = MutableTransaction()
            setInputs(
                mutableTransaction = mutableTransaction,
                originalInputs = originalFullInfo.inputs,
                additionalInputs = inputs
            )
            setOutputs(
                mutableTransaction = mutableTransaction,
                outputs = outputs
            )
            mutableTransaction
        }
    }

    fun replacementTransaction(
        transactionHash: String,
        minFee: Long,
        type: ReplacementType
    ): Triple<MutableTransaction, FullTransactionInfo, List<String>> {
        // TODO: Need to check that this transaction has not been replaced already
        val originalFullInfo = storage.getFullTransactionInfo(transactionHash.toReversedByteArray()) ?: throw BuildError.InvalidTransaction
        check(originalFullInfo.block == null) { "Transaction already in block" }

        val originalFee = originalFullInfo.metadata.fee
        checkNotNull(originalFee) { "No fee for original transaction" }

        check(originalFullInfo.metadata.type != TransactionType.Incoming) { "Can replace only outgoing transaction" }

        val fixedUtxo = originalFullInfo.inputs.mapNotNull { it.previousOutput }

        check(originalFullInfo.inputs.size == fixedUtxo.size) { "No previous output" }

        check(originalFullInfo.inputs.any { it.input.rbfEnabled }) { "Rbf not enabled" }

        val originalSize = sizeCalculator.transactionSize(previousOutputs = fixedUtxo, outputs = originalFullInfo.outputs)

        val originalFeeRate = (originalFee / originalSize).toInt()
        val descendantTransactions = storage.getDescendantTransactionsFullInfo(transactionHash.toReversedByteArray())
        val absoluteFee = descendantTransactions.sumOf { it.metadata.fee ?: 0 }

        check(descendantTransactions.all { it.header.conflictingTxHash == null }) { "Already replaced"}
        check(absoluteFee <= minFee) { "Fee too low" }

        val mutableTransaction = when (type) {
            ReplacementType.SpeedUp -> speedUpReplacement(originalFullInfo, minFee, originalFeeRate, fixedUtxo)
            is ReplacementType.Cancel -> cancelReplacement(originalFullInfo, minFee, originalFee, originalFeeRate, fixedUtxo, type.changeAddress)
        }

        checkNotNull(mutableTransaction) { "Unable to replace" }

        val fullTransaction = mutableTransaction.build()
        metadataExtractor.extract(fullTransaction)
        val metadata = fullTransaction.metadata

        return Triple(
            mutableTransaction,
            FullTransactionInfo(
                block = null,
                header = mutableTransaction.transaction,
                inputs = mutableTransaction.inputsToSign.map {
                    InputWithPreviousOutput(it.input, it.previousOutput)
                },
                outputs = mutableTransaction.outputs,
                metadata = metadata
            ),
            descendantTransactions.map { it.metadata.transactionHash.toReversedHex() }
        )
    }

    fun replacementInfo(transactionHash: String): Pair<FullTransactionInfo, LongRange>? {
        val originalFullInfo = storage.getFullTransactionInfo(transactionHash.toReversedByteArray()) ?: return null
        check(originalFullInfo.block == null) { "Transaction already in block" }
        check(originalFullInfo.metadata.type != TransactionType.Incoming) { "Can replace only outgoing transaction" }

        val descendantTransactions = storage.getDescendantTransactionsFullInfo(transactionHash.toReversedByteArray())
        val absoluteFee = descendantTransactions.sumOf { it.metadata.fee ?: 0 }
        val confirmedUtxoTotalValue = unspentOutputProvider.getConfirmedSpendableUtxo().sumOf { it.output.value }
        val myOutputs = originalFullInfo.outputs.filter { it.publicKeyPath != null && it.pluginId == null }
        val myOutputsTotalValue = myOutputs.sumOf { it.value }

        val feeRange = LongRange(absoluteFee, absoluteFee + myOutputsTotalValue + confirmedUtxoTotalValue)
        return Pair(originalFullInfo, feeRange)
    }

    sealed class BuildError : Throwable() {
        object InvalidTransaction : BuildError()
        object NoPreviousOutput : BuildError()
        object FeeTooLow : BuildError()
        object RbfNotEnabled : BuildError()
        object UnableToReplace : BuildError()
    }
}
