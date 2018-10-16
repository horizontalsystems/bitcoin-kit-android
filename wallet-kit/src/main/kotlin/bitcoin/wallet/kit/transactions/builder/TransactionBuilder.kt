package bitcoin.wallet.kit.transactions.builder

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.hdwallet.Address
import bitcoin.wallet.kit.hdwallet.PublicKey
import bitcoin.wallet.kit.managers.UnspentOutputProvider
import bitcoin.wallet.kit.managers.UnspentOutputSelector
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.models.TransactionInput
import bitcoin.wallet.kit.models.TransactionOutput
import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.wallet.kit.scripts.ScriptBuilder
import bitcoin.wallet.kit.scripts.ScriptType
import bitcoin.wallet.kit.transactions.TransactionSizeCalculator
import io.horizontalsystems.hdwalletkit.HDWallet

class TransactionBuilder(private val networkParameters: NetworkParameters,
                         private val unspentOutputsSelector: UnspentOutputSelector,
                         private val unspentOutputProvider: UnspentOutputProvider,
                         private val scriptBuilder: ScriptBuilder,
                         private val transactionSizeCalculator: TransactionSizeCalculator,
                         private val inputSigner: InputSigner) {

    constructor(realmFactory: RealmFactory, networkParameters: NetworkParameters, wallet: HDWallet)
            : this(networkParameters, UnspentOutputSelector(TransactionSizeCalculator()), UnspentOutputProvider(realmFactory), ScriptBuilder(), TransactionSizeCalculator(), InputSigner(wallet))

    fun fee(value: Int, feeRate: Int, senderPay: Boolean, address: String? = null): Int {
        val outputType = if (address == null) ScriptType.P2PKH else Address(address, networkParameters).scriptType

        val selectedOutputsInfo = unspentOutputsSelector.select(value = value, feeRate = feeRate, outputScriptType = outputType, senderPay = senderPay, unspentOutputs = unspentOutputProvider.allUnspentOutputs())

        val feeWithChangeOutput = if (senderPay) selectedOutputsInfo.fee + transactionSizeCalculator.outputSize(scripType = ScriptType.P2PKH) * feeRate else 0

        return if (selectedOutputsInfo.totalValue > value + feeWithChangeOutput) feeWithChangeOutput else selectedOutputsInfo.fee
    }

    fun buildTransaction(value: Int, toAddress: String, feeRate: Int, senderPay: Boolean, changePubKey: PublicKey, changeScriptType: Int = ScriptType.P2PKH): Transaction {

        val address = Address(toAddress, networkParameters)
        val selectedOutputsInfo = unspentOutputsSelector.select(value = value, feeRate = feeRate, outputScriptType = address.scriptType, senderPay = senderPay, unspentOutputs = unspentOutputProvider.allUnspentOutputs())

        val transaction = Transaction(version = 1, lockTime = 0)

        //add inputs
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

        //add output
        val transactionOutput = TransactionOutput().apply {
            this.value = 0
            this.index = 0
            this.lockingScript = scriptBuilder.lockingScript(address)
            this.scriptType = address.scriptType
            this.address = address.toString()
            this.keyHash = address.hash
        }
        transaction.outputs.add(transactionOutput)

        //calculate fee and add change output if needed
        check(senderPay || selectedOutputsInfo.fee < value) {
            throw TransactionBuilderException.FeeMoreThanValue()
        }

        val receivedValue = if (senderPay) value else value - selectedOutputsInfo.fee
        val sentValue = if (senderPay) value + selectedOutputsInfo.fee else value

        transaction.outputs[0]?.value = receivedValue.toLong()

        if (selectedOutputsInfo.totalValue > sentValue + transactionSizeCalculator.outputSize(scripType = changeScriptType) * feeRate) {
            val changeAddress = Address(Address.Type.P2PKH, changePubKey.publicKeyHash, networkParameters)
            val changeOutput = TransactionOutput().apply {
                this.value = selectedOutputsInfo.totalValue - sentValue
                this.index = 1
                this.lockingScript = scriptBuilder.lockingScript(changeAddress)
                this.scriptType = changeScriptType
                this.address = changeAddress.toString()
                this.keyHash = changeAddress.hash
            }
            transaction.outputs.add(changeOutput)
        }

        //sign inputs
        transaction.inputs.forEachIndexed { index, transactionInput ->
            val sigScriptData = inputSigner.sigScriptData(transaction, index)
            transactionInput?.sigScript = scriptBuilder.unlockingScript(sigScriptData)
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
