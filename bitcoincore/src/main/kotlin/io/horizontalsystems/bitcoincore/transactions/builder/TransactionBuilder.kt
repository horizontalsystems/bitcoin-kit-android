package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.managers.IUnspentOutputSelector
import io.horizontalsystems.bitcoincore.managers.UnspentOutputSelector
import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.serializers.TransactionSerializer
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.InputToSign
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptBuilder
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.hdwalletkit.HDWallet

class TransactionBuilder {
    private val unspentOutputsSelector: IUnspentOutputSelector
    private val scriptBuilder: ScriptBuilder
    private val inputSigner: InputSigner
    private val transactionSizeCalculator: TransactionSizeCalculator

    constructor(wallet: HDWallet, network: Network, unspentOutputSelector: IUnspentOutputSelector, transactionSizeCalculator: TransactionSizeCalculator) {
        this.unspentOutputsSelector = unspentOutputSelector
        this.scriptBuilder = ScriptBuilder()
        this.inputSigner = InputSigner(wallet, network)
        this.transactionSizeCalculator = transactionSizeCalculator
    }

    constructor(unspentOutputsSelector: UnspentOutputSelector, scriptBuilder: ScriptBuilder, inputSigner: InputSigner, transactionSizeCalculator: TransactionSizeCalculator) {
        this.unspentOutputsSelector = unspentOutputsSelector
        this.scriptBuilder = scriptBuilder
        this.inputSigner = inputSigner
        this.transactionSizeCalculator = transactionSizeCalculator
    }

    fun fee(value: Long, feeRate: Int, senderPay: Boolean, address: Address?, changeAddress: Address): Long {
        if (address == null) {
            return unspentOutputsSelector.select(
                    value = value,
                    feeRate = feeRate,
                    outputType = ScriptType.P2PKH,
                    changeType = ScriptType.P2PKH,
                    senderPay = senderPay
            ).fee
        }

        val transaction = buildTransaction(
                value = value,
                feeRate = feeRate,
                senderPay = senderPay,
                address = address,
                changeAddress = changeAddress)

        return TransactionSerializer.serialize(transaction, withWitness = false).size * feeRate.toLong()
    }

    fun buildTransaction(value: Long, address: Address, feeRate: Int, senderPay: Boolean, changeAddress: Address): FullTransaction {

        val selectedOutputsInfo = unspentOutputsSelector.select(
                value = value,
                feeRate = feeRate,
                outputType = address.scriptType,
                changeType = changeAddress.scriptType,
                senderPay = senderPay
        )

        if (!senderPay && selectedOutputsInfo.fee > value) {
            throw BuilderException.FeeMoreThanValue()
        }

        val transaction = Transaction(version = 1, lockTime = 0)
        val inputsToSign = mutableListOf<InputToSign>()
        val outputs = mutableListOf<TransactionOutput>()

        //  add inputs
        for (unspentOutput in selectedOutputsInfo.outputs) {
            val previousOutput = unspentOutput.output
            val transactionInput = TransactionInput(previousOutput.transactionHash, previousOutput.index.toLong())

            if (unspentOutput.output.scriptType == ScriptType.P2WPKH) {
                unspentOutput.output.keyHash = unspentOutput.output.keyHash?.drop(2)?.toByteArray()
            }

            inputsToSign.add(InputToSign(transactionInput, previousOutput, unspentOutput.publicKey))
        }

        //  calculate fee and add change output if needed
        val receivedValue = if (senderPay) value else value - selectedOutputsInfo.fee
        val sentValue = if (senderPay) value + selectedOutputsInfo.fee else value

        //  add output
        outputs.add(TransactionOutput(receivedValue, 0, scriptBuilder.lockingScript(address), address.scriptType, address.string, address.hash))

        if (selectedOutputsInfo.addChangeOutput) {
            outputs.add(TransactionOutput(selectedOutputsInfo.totalValue - sentValue, 1, scriptBuilder.lockingScript(changeAddress), changeAddress.scriptType, changeAddress.string, changeAddress.hash))
        }

        // sign inputs
        inputsToSign.forEachIndexed { index, inputToSign ->
            val unspentOutput = selectedOutputsInfo.outputs[index]
            val output = unspentOutput.output
            val sigScriptData = inputSigner.sigScriptData(transaction, inputsToSign, outputs, index)

            when (output.scriptType) {
                ScriptType.P2WPKH -> {
                    transaction.segwit = true
                    inputToSign.input.witness = sigScriptData
                }

                ScriptType.P2WPKHSH -> {
                    transaction.segwit = true
                    val witnessProgram = OpCodes.scriptWPKH(unspentOutput.publicKey.publicKeyHash)

                    inputToSign.input.sigScript = scriptBuilder.unlockingScript(listOf(witnessProgram))
                    inputToSign.input.witness = sigScriptData
                }

                else -> inputToSign.input.sigScript = scriptBuilder.unlockingScript(sigScriptData)
            }
        }

        transaction.status = Transaction.Status.NEW
        transaction.isMine = true
        transaction.isOutgoing = true

        return FullTransaction(transaction, inputsToSign.map { it.input }, outputs)
    }

    fun buildTransaction(unspentOutput: UnspentOutput, address: Address, feeRate: Int, signatureScriptFunction: (ByteArray, ByteArray) -> ByteArray): FullTransaction {

        //  calculate fee
        val emptySignature = ByteArray(transactionSizeCalculator.signatureLength)
        val emptyPublicKey = ByteArray(transactionSizeCalculator.pubKeyLength)

        val transactionSize = transactionSizeCalculator.transactionSize(listOf(ScriptType.P2SH), listOf(ScriptType.P2PKH)) + signatureScriptFunction.invoke(emptySignature, emptyPublicKey).size

        val fee = transactionSize * feeRate
        if (fee > unspentOutput.output.value) {
            throw BuilderException.FeeMoreThanValue()
        }

        //  add input without unlocking scripts
        val transactionInput = TransactionInput(unspentOutput.output.transactionHash, unspentOutput.output.index.toLong())
        if (unspentOutput.output.scriptType == ScriptType.P2WPKH) {
            unspentOutput.output.keyHash = unspentOutput.output.keyHash?.drop(2)?.toByteArray()
        }
        val inputToSign = InputToSign(transactionInput, unspentOutput.output, unspentOutput.publicKey)

        //  calculate receiveValue
        val receivedValue = unspentOutput.output.value - fee

        //  add output
        val output = TransactionOutput(receivedValue, 0, scriptBuilder.lockingScript(address), address.scriptType, address.string, address.hash)

        //  build transaction
        val transaction = Transaction(version = 1, lockTime = 0)

        //  sign input
        val sigScriptData = inputSigner.sigScriptData(transaction, listOf(inputToSign), listOf(output), 0)
        inputToSign.input.sigScript = signatureScriptFunction.invoke(sigScriptData[0], sigScriptData[1])

        transaction.status = Transaction.Status.NEW
        transaction.isMine = true
        transaction.isOutgoing = false

        return FullTransaction(transaction, listOf(inputToSign.input), listOf(output))
    }

    open class BuilderException : Exception() {
        class FeeMoreThanValue : BuilderException()
    }
}
