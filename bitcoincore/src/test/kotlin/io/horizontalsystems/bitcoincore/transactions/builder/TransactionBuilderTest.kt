package io.horizontalsystems.bitcoincore.transactions.builder

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.Fixtures
import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.managers.SelectedUnspentOutputInfo
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptBuilder
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.AddressConverterChain
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TransactionBuilderTest : Spek({

    lateinit var previousTransaction: FullTransaction
    lateinit var unspentOutputs: SelectedUnspentOutputInfo
    lateinit var transactionBuilder: TransactionBuilder

    val publicKey = mock<PublicKey>()
    val unspentOutput = mock<UnspentOutput>()
    val scriptBuilder = mock<ScriptBuilder>()
    val transactionSizeCalculator = mock<TransactionSizeCalculator>()
    val inputSigner = mock<InputSigner>()

    val addressConverter = AddressConverterChain().also {
        it.prependConverter(Base58AddressConverter(111, 196))
    }

    val toAddressP2PKH = "mmLB5DvGbsb4krT9PJ7WrKmv8DkyvNx1ne"
    val toAddressP2SH = "2MyQWMrsLsqAMSUeusduAzN6pWuH2V27ykE"
    val addressP2PKH = addressConverter.convert(toAddressP2PKH)
    val addressChange = addressConverter.convert(toAddressP2PKH)
    val addressP2SH = addressConverter.convert(toAddressP2SH)

    val txValue = 93_417_732L
    val feeRate = 5406
    val fee = 1_032_655L
    val unlockingScript = "473044022018f03676d057a3cb350d9778697ff61da47b813c82fe9fb0f2ea87b231fb865b02200706f5cbbc5ebae6f7bd77e346767bce11c8476aea607671d7321e86a3186ec1012102ce0ef85579f055e2184c935e75e71458db8c4b759cd455b0aa5d91761794eef0".hexToByteArray()

    beforeEachTest {

        transactionBuilder = TransactionBuilder(scriptBuilder, inputSigner)

        previousTransaction = Fixtures.transactionP2PKH

        whenever(unspentOutput.output).thenReturn(previousTransaction.outputs[0])
        whenever(unspentOutput.publicKey).thenReturn(publicKey)

        unspentOutputs = SelectedUnspentOutputInfo(listOf(unspentOutput), previousTransaction.outputs[0].value, fee, true)

        // receive address locking script P2PKH
        whenever(scriptBuilder.lockingScript(argThat { hash.contentEquals("3fc6d8a8215dd60e42a3916c4def39f40d322e29".hexToByteArray()) })).thenReturn("76a91437a9bfe84d9e4883ace248509bbf14c9d72af01788ac".hexToByteArray())
        // receive address locking script P2SH
        whenever(scriptBuilder.lockingScript(argThat { hash.contentEquals("43922a3f1dc4569f9eccce9a71549d5acabbc0ca".hexToByteArray()) })).thenReturn("76a91437a9bfe84d9e4883ace248509bbf14c9d72af01788ac".hexToByteArray())
        // change address locking script
        whenever(scriptBuilder.lockingScript(argThat { hash.contentEquals("563e1365e6567bb0115a5158bfc94fe834067fd6".hexToByteArray()) })).thenReturn("76a914d1997b4cc28ae0e432461479b5e89106f9d4eef488ac".hexToByteArray())

        whenever(inputSigner.sigScriptData(any(), any(), any(), any())).thenReturn(listOf())
        whenever(scriptBuilder.unlockingScript(any())).thenReturn(unlockingScript)
    }

    describe("#buildTransaction") {

        it("P2PKH_SenderPay") {
            unspentOutputs.outputs[0].output.scriptType = ScriptType.P2PKH

            val transaction = transactionBuilder.buildTransaction(txValue, unspentOutputs.outputs, unspentOutputs.fee, true, addressP2PKH, null)

            assertTrue(transaction.header.isMine)
            assertEquals(Transaction.Status.NEW, transaction.header.status)

            assertEquals(1, transaction.inputs.size)
            // assertEquals(unspentOutputs.outputs[0], transaction.inputs[0]?.previousOutput)

            assertEquals(1, transaction.outputs.size)

            assertEquals(toAddressP2PKH, transaction.outputs[0].address)
            assertEquals(txValue, transaction.outputs[0].value)
        }

        it("P2PKH_ReceiverPay") {
            unspentOutputs.outputs[0].output.scriptType = ScriptType.P2PKH

            val transaction = transactionBuilder.buildTransaction(txValue, unspentOutputs.outputs, unspentOutputs.fee, false, addressP2PKH, null)

            assertTrue(transaction.header.isMine)
            assertEquals(Transaction.Status.NEW, transaction.header.status)

            assertEquals(1, transaction.inputs.size)
            // assertEquals(unspentOutputs.outputs[0], transaction.inputs[0]?.previousOutput)
            // assertNull(transaction.outputs[0].publicKey)

            assertEquals(1, transaction.outputs.size)

            assertEquals(toAddressP2PKH, transaction.outputs[0].address)
            assertEquals((txValue - fee), transaction.outputs[0].value)
        }

//        it("P2SH") {
//            unspentOutputs.outputs[0].output.scriptType = ScriptType.P2SH
//
//            val transaction = transactionBuilder.buildTransaction(txValue, unspentOutputs.outputs, unspentOutputs.fee, false, addressP2SH, null)
//
//            assertTrue(transaction.header.isMine)
//            assertEquals(Transaction.Status.NEW, transaction.header.status)
//
//            assertEquals(1, transaction.inputs.size)
//            // assertEquals(unspentOutputs.outputs[0], transaction.inputs[0]?.previousOutput)
//
//            assertEquals(1, transaction.outputs.size)
//
//            assertEquals(toAddressP2SH, transaction.outputs[0].address)
//            assertEquals((txValue - fee), transaction.outputs[0].value)
//        }

        it("WithoutChangeOutput") {
            unspentOutputs.outputs[0].output.scriptType = ScriptType.P2PKH

            val txValue = unspentOutputs.outputs[0].output.value

            val transaction = transactionBuilder.buildTransaction(txValue, unspentOutputs.outputs, unspentOutputs.fee, false, addressP2PKH, null)

            assertEquals(1, transaction.inputs.size)
            //assertEquals(unspentOutputs.outputs[0], transaction.inputs[0]?.previousOutput)

            assertEquals(1, transaction.outputs.size)
            assertEquals(toAddressP2PKH, transaction.outputs[0].address)
            assertEquals((txValue - fee), transaction.outputs[0].value)
        }

        it("ChangeNotAddedForDust") {
            unspentOutputs.outputs[0].output.scriptType = ScriptType.P2PKH

            val txValue = unspentOutputs.outputs[0].output.value - transactionSizeCalculator.outputSize(scripType = ScriptType.P2PKH) * feeRate

            val transaction = transactionBuilder.buildTransaction(txValue, unspentOutputs.outputs, unspentOutputs.fee, false, addressP2PKH, null)

            assertEquals(1, transaction.inputs.size)
            //assertEquals(unspentOutputs.outputs[0], transaction.inputs[0]?.previousOutput)

            assertEquals(1, transaction.outputs.size)
            assertEquals(toAddressP2PKH, transaction.outputs[0].address)
            assertEquals((txValue - fee), transaction.outputs[0].value)
        }

        it("InputsSigned") {
            unspentOutputs.outputs[0].output.scriptType = ScriptType.P2PKH

            val transaction = transactionBuilder.buildTransaction(txValue, unspentOutputs.outputs, unspentOutputs.fee, false, addressP2PKH, null)

            assertEquals(unlockingScript.toHexString(), transaction.inputs[0].sigScript.toHexString())
        }

    }

})
