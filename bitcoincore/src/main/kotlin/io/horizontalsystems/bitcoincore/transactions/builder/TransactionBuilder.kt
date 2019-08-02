package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.core.IAddressKeyHashConverter
import io.horizontalsystems.bitcoincore.managers.AddressManager
import io.horizontalsystems.bitcoincore.managers.IUnspentOutputSelector
import io.horizontalsystems.bitcoincore.managers.UnspentOutputProvider
import io.horizontalsystems.bitcoincore.managers.UnspentOutputSelector
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
import io.horizontalsystems.bitcoincore.utils.IAddressConverter
import io.horizontalsystems.hdwalletkit.HDWallet

class TransactionBuilder {
    private val addressConverter: IAddressConverter
    private val unspentOutputsSelector: IUnspentOutputSelector
    private val scriptBuilder: ScriptBuilder
    private val inputSigner: InputSigner
    private val addressManager: AddressManager
    private val transactionSizeCalculator: TransactionSizeCalculator
    private val addressKeyHashConverter: IAddressKeyHashConverter?

    constructor(addressConverter: IAddressConverter, wallet: HDWallet, network: Network, addressManager: AddressManager, unspentOutputSelector: IUnspentOutputSelector, transactionSizeCalculator: TransactionSizeCalculator, addressKeyHashConverter: IAddressKeyHashConverter?) {
        this.addressConverter = addressConverter
        this.addressManager = addressManager
        this.unspentOutputsSelector = unspentOutputSelector
        this.scriptBuilder = ScriptBuilder()
        this.inputSigner = InputSigner(wallet, network)
        this.transactionSizeCalculator = transactionSizeCalculator
        this.addressKeyHashConverter = addressKeyHashConverter
    }

    constructor(addressConverter: IAddressConverter, unspentOutputsSelector: UnspentOutputSelector, unspentOutputProvider: UnspentOutputProvider, scriptBuilder: ScriptBuilder, inputSigner: InputSigner, addressManager: AddressManager, transactionSizeCalculator: TransactionSizeCalculator) {
        this.addressManager = addressManager
        this.addressConverter = addressConverter
        this.unspentOutputsSelector = unspentOutputsSelector
        this.scriptBuilder = scriptBuilder
        this.inputSigner = inputSigner
        this.transactionSizeCalculator = transactionSizeCalculator
        this.addressKeyHashConverter = null
    }

    fun fee(value: Long, feeRate: Int, senderPay: Boolean, address: String? = null, changeScriptType: Int): Long {
        val estimatedFee = if (address == null) {
            true
        } else try { // if address is valid then calculate actual fee
            addressConverter.convert(address)
            false
        } catch (e: Exception) {
            true
        }

        if (estimatedFee) {
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
                toAddress = address!!,
                changeScriptType = changeScriptType
        )

        return TransactionSerializer.serialize(transaction, withWitness = false).size * feeRate.toLong()
    }

    fun buildTransaction(value: Long, toAddress: String, feeRate: Int, senderPay: Boolean, changeScriptType: Int): FullTransaction {

        val address = addressConverter.convert(toAddress)
        val selectedOutputsInfo = unspentOutputsSelector.select(
                value = value,
                feeRate = feeRate,
                outputType = address.scriptType,
                changeType = changeScriptType,
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
            val keyHash = addressManager.changePublicKey().publicKeyHash
            val correctKeyHash = addressKeyHashConverter?.convert(keyHash, changeScriptType) ?: keyHash

            val changeAddress = addressConverter.convert(correctKeyHash, changeScriptType)
            outputs.add(TransactionOutput(selectedOutputsInfo.totalValue - sentValue, 1, scriptBuilder.lockingScript(changeAddress), changeScriptType, changeAddress.string, changeAddress.hash))
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

    fun buildTransaction(unspentOutput: UnspentOutput, addressStr: String, feeRate: Int, signatureScriptFunction: (ByteArray, ByteArray) -> ByteArray): FullTransaction {
        val address = addressConverter.convert(addressStr)

        //  calculate fee
        val emptySignature = ByteArray(transactionSizeCalculator.signatureLength)
        val emptyPublicKey = ByteArray(transactionSizeCalculator.pubKeyLength)

        val transactionSize = transactionSizeCalculator.transactionSize(listOf(ScriptType.P2SH), listOf(ScriptType.P2PKH)) +
                signatureScriptFunction.invoke(emptySignature, emptyPublicKey).size
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

        // calculate receiveValue
        val receivedValue = unspentOutput.output.value - fee

        // add output
        val output = TransactionOutput(receivedValue, 0, scriptBuilder.lockingScript(address), address.scriptType, address.string, address.hash)

        // build transaction
        val transaction = Transaction(version = 1, lockTime = 0)

        // sign input
        val sigScriptData = inputSigner.sigScriptData(transaction, listOf(inputToSign), listOf(output), 0)
        inputToSign.input.sigScript = signatureScriptFunction.invoke(sigScriptData[0], sigScriptData[1])

        transaction.status = Transaction.Status.NEW
        transaction.isMine = true
        transaction.isOutgoing = false

        return FullTransaction(transaction, listOf(inputToSign.input), listOf(output))
    }

    open class BuilderException : Exception() {
        class NoPreviousTransaction : BuilderException()
        class FeeMoreThanValue : BuilderException()
    }

}
