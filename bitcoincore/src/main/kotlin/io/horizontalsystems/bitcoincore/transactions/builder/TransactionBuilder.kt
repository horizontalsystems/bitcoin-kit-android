package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.managers.AddressManager
import io.horizontalsystems.bitcoincore.managers.UnspentOutputProvider
import io.horizontalsystems.bitcoincore.managers.UnspentOutputSelector
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.serializers.TransactionSerializer
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.InputToSign
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptBuilder
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.IAddressConverter
import io.horizontalsystems.hdwalletkit.HDWallet

class TransactionBuilder {
    private val addressConverter: IAddressConverter
    private val unspentOutputsSelector: UnspentOutputSelector
    private val unspentOutputProvider: UnspentOutputProvider
    private val scriptBuilder: ScriptBuilder
    private val inputSigner: InputSigner
    private val addressManager: AddressManager

    constructor(addressConverter: IAddressConverter, wallet: HDWallet, network: Network, addressManager: AddressManager, unspentOutputProvider: UnspentOutputProvider) {
        this.addressConverter = addressConverter
        this.addressManager = addressManager
        this.unspentOutputsSelector = UnspentOutputSelector(TransactionSizeCalculator())
        this.unspentOutputProvider = unspentOutputProvider
        this.scriptBuilder = ScriptBuilder()
        this.inputSigner = InputSigner(wallet, network)
    }

    constructor(addressConverter: IAddressConverter, unspentOutputsSelector: UnspentOutputSelector, unspentOutputProvider: UnspentOutputProvider, scriptBuilder: ScriptBuilder, inputSigner: InputSigner, addressManager: AddressManager) {
        this.addressManager = addressManager
        this.addressConverter = addressConverter
        this.unspentOutputsSelector = unspentOutputsSelector
        this.unspentOutputProvider = unspentOutputProvider
        this.scriptBuilder = scriptBuilder
        this.inputSigner = inputSigner
    }

    fun fee(value: Long, feeRate: Int, senderPay: Boolean, address: String? = null): Long {
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
                    senderPay = senderPay,
                    unspentOutputs = unspentOutputProvider.allUnspentOutputs()
            ).fee
        }

        val transaction = buildTransaction(
                value = value,
                feeRate = feeRate,
                senderPay = senderPay,
                toAddress = address!!
        )

        return TransactionSerializer.serialize(transaction, withWitness = false).size * feeRate.toLong()
    }

    fun buildTransaction(value: Long, toAddress: String, feeRate: Int, senderPay: Boolean): FullTransaction {

        val address = addressConverter.convert(toAddress)
        val changePubKey = addressManager.changePublicKey()
        val changeScriptType = ScriptType.P2PKH
        val selectedOutputsInfo = unspentOutputsSelector.select(
                value = value,
                feeRate = feeRate,
                outputType = address.scriptType,
                changeType = changeScriptType,
                senderPay = senderPay,
                unspentOutputs = unspentOutputProvider.allUnspentOutputs()
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

            inputsToSign.add(InputToSign(transactionInput, previousOutput, unspentOutput.publicKey))
        }

        //  calculate fee and add change output if needed
        val receivedValue = if (senderPay) value else value - selectedOutputsInfo.fee
        val sentValue = if (senderPay) value + selectedOutputsInfo.fee else value

        //  add output
        outputs.add(TransactionOutput(receivedValue, 0, scriptBuilder.lockingScript(address), address.scriptType, address.string, address.hash))

        if (selectedOutputsInfo.addChangeOutput) {
            val changeAddress = addressConverter.convert(changePubKey.publicKeyHash, changeScriptType)
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
                    val witnessProgram = OpCodes.push(0) + OpCodes.push(unspentOutput.publicKey.publicKeyHash)

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

    open class BuilderException : Exception() {
        class NoPreviousTransaction : BuilderException()
        class FeeMoreThanValue : BuilderException()
    }

}
