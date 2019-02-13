package io.horizontalsystems.bitcoinkit.transactions.builder

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.managers.AddressManager
import io.horizontalsystems.bitcoinkit.managers.UnspentOutputProvider
import io.horizontalsystems.bitcoinkit.managers.UnspentOutputSelector
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.models.TransactionInput
import io.horizontalsystems.bitcoinkit.models.TransactionOutput
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.bitcoinkit.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoinkit.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptBuilder
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoinkit.utils.AddressConverter
import io.horizontalsystems.hdwalletkit.HDWallet
import io.realm.Realm

class TransactionBuilder {
    private val addressConverter: AddressConverter
    private val unspentOutputsSelector: UnspentOutputSelector
    private val unspentOutputProvider: UnspentOutputProvider
    private val scriptBuilder: ScriptBuilder
    private val inputSigner: InputSigner
    private val addressManager: AddressManager
    private val realmFactory: RealmFactory

    constructor(realmFactory: RealmFactory, addressConverter: AddressConverter, wallet: HDWallet, network: Network, addressManager: AddressManager, unspentOutputProvider: UnspentOutputProvider) {
        this.realmFactory = realmFactory
        this.addressConverter = addressConverter
        this.addressManager = addressManager
        this.unspentOutputsSelector = UnspentOutputSelector(TransactionSizeCalculator())
        this.unspentOutputProvider = unspentOutputProvider
        this.scriptBuilder = ScriptBuilder()
        this.inputSigner = InputSigner(wallet, network)
    }

    constructor(realmFactory: RealmFactory, addressConverter: AddressConverter, unspentOutputsSelector: UnspentOutputSelector, unspentOutputProvider: UnspentOutputProvider, scriptBuilder: ScriptBuilder, inputSigner: InputSigner, addressManager: AddressManager) {
        this.realmFactory = realmFactory
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

        realmFactory.realm.use { realm ->
            if (estimatedFee) {
                return unspentOutputsSelector.select(
                        value = value,
                        feeRate = feeRate,
                        outputType = ScriptType.P2PKH,
                        changeType = ScriptType.P2PKH,
                        senderPay = senderPay,
                        outputs = unspentOutputProvider.allUnspentOutputs(realm)
                ).fee
            }

            val transaction = buildTransaction(
                    realm = realm,
                    value = value,
                    feeRate = feeRate,
                    senderPay = senderPay,
                    toAddress = address!!
            )

            return transaction.toByteArray(withWitness = false).size * feeRate.toLong()
        }
    }

    fun buildTransaction(value: Long, toAddress: String, feeRate: Int, senderPay: Boolean, realm: Realm): Transaction {

        val address = addressConverter.convert(toAddress)
        val changePubKey = addressManager.changePublicKey(realm)
        val changeScriptType = ScriptType.P2PKH
        val selectedOutputsInfo = unspentOutputsSelector.select(
                value = value,
                feeRate = feeRate,
                outputType = address.scriptType,
                changeType = changeScriptType,
                senderPay = senderPay,
                outputs = unspentOutputProvider.allUnspentOutputs(realm)
        )

        if (!senderPay && selectedOutputsInfo.fee > value) {
            throw BuilderException.FeeMoreThanValue()
        }

        val transaction = Transaction(version = 1, lockTime = 0)

        // add inputs
        for (output in selectedOutputsInfo.outputs) {
            val previousTx = checkNotNull(output.transaction) {
                throw BuilderException.NoPreviousTransaction()
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

        //  calculate fee and add change output if needed
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
        transaction.isOutgoing = true
        transaction.setHashes()

        return transaction
    }

    open class BuilderException : Exception() {
        class NoPreviousTransaction : BuilderException()
        class FeeMoreThanValue : BuilderException()
    }

}
