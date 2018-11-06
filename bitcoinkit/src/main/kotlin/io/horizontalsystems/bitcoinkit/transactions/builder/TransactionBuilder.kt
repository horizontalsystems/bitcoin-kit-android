package io.horizontalsystems.bitcoinkit.transactions.builder

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.managers.UnspentOutputProvider
import io.horizontalsystems.bitcoinkit.managers.UnspentOutputSelector
import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.models.TransactionInput
import io.horizontalsystems.bitcoinkit.models.TransactionOutput
import io.horizontalsystems.bitcoinkit.scripts.OpCodes
import io.horizontalsystems.bitcoinkit.scripts.ScriptBuilder
import io.horizontalsystems.bitcoinkit.scripts.ScriptType
import io.horizontalsystems.bitcoinkit.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoinkit.utils.AddressConverter
import io.horizontalsystems.hdwalletkit.HDWallet

class TransactionBuilder {
    private val addressConverter: AddressConverter
    private val unspentOutputsSelector: UnspentOutputSelector
    private val unspentOutputProvider: UnspentOutputProvider
    private val scriptBuilder: ScriptBuilder
    private val inputSigner: InputSigner

    constructor(realmFactory: RealmFactory, addressConverter: AddressConverter, wallet: HDWallet) {
        this.addressConverter = addressConverter
        this.unspentOutputsSelector = UnspentOutputSelector(TransactionSizeCalculator())
        this.unspentOutputProvider = UnspentOutputProvider(realmFactory)
        this.scriptBuilder = ScriptBuilder()
        this.inputSigner = InputSigner(wallet)
    }

    constructor(addressConverter: AddressConverter, unspentOutputsSelector: UnspentOutputSelector, unspentOutputProvider: UnspentOutputProvider, scriptBuilder: ScriptBuilder, inputSigner: InputSigner) {
        this.addressConverter = addressConverter
        this.unspentOutputsSelector = unspentOutputsSelector
        this.unspentOutputProvider = unspentOutputProvider
        this.scriptBuilder = scriptBuilder
        this.inputSigner = inputSigner
    }

    fun fee(value: Int, feeRate: Int, senderPay: Boolean, address: String? = null): Int {
        val outputType = if (address == null) ScriptType.P2PKH else addressConverter.convert(address).scriptType

        return unspentOutputsSelector.select(
                value = value,
                feeRate = feeRate,
                outputType = outputType,
                senderPay = senderPay,
                outputs = unspentOutputProvider.allUnspentOutputs()
        ).fee
    }

    fun buildTransaction(value: Int, toAddress: String, feeRate: Int, senderPay: Boolean, changePubKey: PublicKey, changeScriptType: Int = ScriptType.P2PKH): Transaction {

        val address = addressConverter.convert(toAddress)
        val selectedOutputsInfo = unspentOutputsSelector.select(
                value = value,
                feeRate = feeRate,
                outputType = address.scriptType,
                changeType = changeScriptType,
                senderPay = senderPay,
                outputs = unspentOutputProvider.allUnspentOutputs()
        )

        val transaction = Transaction(version = 1, lockTime = 0)

        // add inputs
        for (output in selectedOutputsInfo.outputs) {
            val previousTx = checkNotNull(output.transaction) {
                throw TransactionBuilderException.NoPreviousTransaction()
            }
            val txInput = TransactionInput().apply {
                previousOutputHash = previousTx.hash
                previousOutputHexReversed = previousTx.hashHexReversed
                previousOutputIndex = output.index.toLong()
            }
            txInput.previousOutput = output
            transaction.inputs.add(txInput)
        }

        // add output
        transaction.outputs.add(TransactionOutput().apply {
            this.value = 0
            this.index = 0
            this.lockingScript = scriptBuilder.lockingScript(address)
            this.scriptType = address.scriptType
            this.address = address.string
            this.keyHash = address.hash
        })

        // calculate fee and add change output if needed
        if (!senderPay && selectedOutputsInfo.fee > value) {
            throw TransactionBuilderException.FeeMoreThanValue()
        }

        val receivedValue = if (senderPay) value else value - selectedOutputsInfo.fee
        val sentValue = if (senderPay) value + selectedOutputsInfo.fee else value

        transaction.outputs[0]?.value = receivedValue.toLong()

        if (selectedOutputsInfo.addChangeOutput) {
            val changeAddress = addressConverter.convert(changePubKey.publicKeyHash, changeScriptType)
            transaction.outputs.add(TransactionOutput().apply {
                this.value = selectedOutputsInfo.totalValue - sentValue
                this.index = 1
                this.lockingScript = scriptBuilder.lockingScript(changeAddress)
                this.scriptType = changeScriptType
                this.address = changeAddress.string
                this.keyHash = changeAddress.hash
                this.publicKey = changePubKey
            })
        }

        // sign inputs
        transaction.inputs.forEachIndexed { index, input ->
            val output = selectedOutputsInfo.outputs[index]
            val sigScriptData = inputSigner.sigScriptData(transaction, index)

            when (output.scriptType) {
                ScriptType.P2WPKH -> {
                    transaction.segwit = true
                    input.witness.addAll(sigScriptData)
                }

                ScriptType.P2WPKHSH -> {
                    val pubKey = checkNotNull(output.publicKey)

                    transaction.segwit = true
                    val witnessProgram = OpCodes.push(0) + OpCodes.push(pubKey.publicKeyHash)

                    input.sigScript = scriptBuilder.unlockingScript(listOf(witnessProgram))
                    input.witness.addAll(sigScriptData)
                }

                else -> input.sigScript = scriptBuilder.unlockingScript(sigScriptData)
            }
        }

        transaction.status = Transaction.Status.NEW
        transaction.isMine = true
        transaction.setHashes()

        return transaction
    }

    open class TransactionBuilderException : Exception() {
        class NoPreviousTransaction : TransactionBuilderException()
        class FeeMoreThanValue : TransactionBuilderException()
    }

}
