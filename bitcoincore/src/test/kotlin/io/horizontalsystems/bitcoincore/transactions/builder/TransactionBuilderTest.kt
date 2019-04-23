package io.horizontalsystems.bitcoincore.transactions.builder

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoincore.Fixtures
import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.managers.AddressManager
import io.horizontalsystems.bitcoincore.managers.SelectedUnspentOutputInfo
import io.horizontalsystems.bitcoincore.managers.UnspentOutputProvider
import io.horizontalsystems.bitcoincore.managers.UnspentOutputSelector
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
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class TransactionBuilderTest {

    private val publicKey = Mockito.mock(PublicKey::class.java)
    private val unspentOutput = Mockito.mock(UnspentOutput::class.java)
    private val unspentOutputSelector = Mockito.mock(UnspentOutputSelector::class.java)
    private val unspentOutputProvider = Mockito.mock(UnspentOutputProvider::class.java)
    private val scriptBuilder = Mockito.mock(ScriptBuilder::class.java)
    private val transactionSizeCalculator = Mockito.mock(TransactionSizeCalculator::class.java)
    private val inputSigner = Mockito.mock(InputSigner::class.java)
    private val addressManager = Mockito.mock(AddressManager::class.java)

    private lateinit var previousTransaction: FullTransaction
    private lateinit var unspentOutputs: SelectedUnspentOutputInfo
    private lateinit var transactionBuilder: TransactionBuilder
    private lateinit var addressConverter: AddressConverterChain

    private val toAddressP2PKH = "mmLB5DvGbsb4krT9PJ7WrKmv8DkyvNx1ne"
    private val toAddressP2SH = "2MyQWMrsLsqAMSUeusduAzN6pWuH2V27ykE"

    private val txValue = 93_417_732L
    private val feeRate = 5406
    private val fee = 1_032_655L
    private val unlockingScript = "473044022018f03676d057a3cb350d9778697ff61da47b813c82fe9fb0f2ea87b231fb865b02200706f5cbbc5ebae6f7bd77e346767bce11c8476aea607671d7321e86a3186ec1012102ce0ef85579f055e2184c935e75e71458db8c4b759cd455b0aa5d91761794eef0".hexToByteArray()

    @Before
    fun setUp() {
        addressConverter = AddressConverterChain()
        addressConverter.prependConverter(Base58AddressConverter(111, 196))

        transactionBuilder = TransactionBuilder(addressConverter, unspentOutputSelector, unspentOutputProvider, scriptBuilder, inputSigner, addressManager)

        previousTransaction = Fixtures.transactionP2PKH

        whenever(unspentOutput.output).thenReturn(previousTransaction.outputs[0])
        whenever(unspentOutput.publicKey).thenReturn(publicKey)

        unspentOutputs = SelectedUnspentOutputInfo(listOf(unspentOutput), previousTransaction.outputs[0].value, fee, false)

        whenever(unspentOutputProvider.allUnspentOutputs()).thenReturn(unspentOutputs.outputs)
        whenever(unspentOutputSelector.select(any(), any(), any(), any(), any(), any())).thenReturn(unspentOutputs)
        whenever(transactionSizeCalculator.outputSize(any())).thenReturn(34)

        // receive address locking script P2PKH
        whenever(scriptBuilder.lockingScript(argThat { hash.contentEquals("3fc6d8a8215dd60e42a3916c4def39f40d322e29".hexToByteArray()) })).thenReturn("76a91437a9bfe84d9e4883ace248509bbf14c9d72af01788ac".hexToByteArray())
        // receive address locking script P2SH
        whenever(scriptBuilder.lockingScript(argThat { hash.contentEquals("43922a3f1dc4569f9eccce9a71549d5acabbc0ca".hexToByteArray()) })).thenReturn("76a91437a9bfe84d9e4883ace248509bbf14c9d72af01788ac".hexToByteArray())
        // change address locking script
        whenever(scriptBuilder.lockingScript(argThat { hash.contentEquals("563e1365e6567bb0115a5158bfc94fe834067fd6".hexToByteArray()) })).thenReturn("76a914d1997b4cc28ae0e432461479b5e89106f9d4eef488ac".hexToByteArray())

        whenever(inputSigner.sigScriptData(any(), any(), any(), any())).thenReturn(listOf())
        whenever(scriptBuilder.unlockingScript(any())).thenReturn(unlockingScript)
    }

    @Test
    fun buildTransaction_P2PKH_SenderPay() {
        val transaction = transactionBuilder.buildTransaction(txValue, toAddressP2PKH, feeRate, true)

        assertTrue(transaction.header.isMine)
        assertEquals(Transaction.Status.NEW, transaction.header.status)

        assertEquals(1, transaction.inputs.size)
        // assertEquals(unspentOutputs.outputs[0], transaction.inputs[0]?.previousOutput)

        assertEquals(1, transaction.outputs.size)

        assertEquals(toAddressP2PKH, transaction.outputs[0].address)
        assertEquals(txValue, transaction.outputs[0].value)
    }

    @Test
    fun buildTransaction_P2PKH_ReceiverPay() {
        val transaction = transactionBuilder.buildTransaction(txValue, toAddressP2PKH, feeRate, false)

        assertTrue(transaction.header.isMine)
        assertEquals(Transaction.Status.NEW, transaction.header.status)

        assertEquals(1, transaction.inputs.size)
        // assertEquals(unspentOutputs.outputs[0], transaction.inputs[0]?.previousOutput)
        // assertNull(transaction.outputs[0].publicKey)

        assertEquals(1, transaction.outputs.size)

        assertEquals(toAddressP2PKH, transaction.outputs[0].address)
        assertEquals((txValue - fee), transaction.outputs[0].value)
    }

    @Test
    fun buildTransaction_P2SH() {
        val transaction = transactionBuilder.buildTransaction(txValue, toAddressP2SH, feeRate, false)

        assertTrue(transaction.header.isMine)
        assertEquals(Transaction.Status.NEW, transaction.header.status)

        assertEquals(1, transaction.inputs.size)
        // assertEquals(unspentOutputs.outputs[0], transaction.inputs[0]?.previousOutput)

        assertEquals(1, transaction.outputs.size)

        assertEquals(toAddressP2SH, transaction.outputs[0].address)
        assertEquals((txValue - fee), transaction.outputs[0].value)
    }

    @Test
    fun buildTransaction_WithoutChangeOutput() {
        val txValue = unspentOutputs.outputs[0].output.value

        val transaction = transactionBuilder.buildTransaction(txValue, toAddressP2PKH, feeRate, false)

        assertEquals(1, transaction.inputs.size)
        //assertEquals(unspentOutputs.outputs[0], transaction.inputs[0]?.previousOutput)

        assertEquals(1, transaction.outputs.size)
        assertEquals(toAddressP2PKH, transaction.outputs[0].address)
        assertEquals((txValue - fee), transaction.outputs[0].value)
    }

    @Test
    fun buildTransaction_ChangeNotAddedForDust() {
        val txValue = unspentOutputs.outputs[0].output.value - transactionSizeCalculator.outputSize(scripType = ScriptType.P2PKH) * feeRate

        val transaction = transactionBuilder.buildTransaction(txValue, toAddressP2PKH, feeRate, false)

        assertEquals(1, transaction.inputs.size)
        //assertEquals(unspentOutputs.outputs[0], transaction.inputs[0]?.previousOutput)

        assertEquals(1, transaction.outputs.size)
        assertEquals(toAddressP2PKH, transaction.outputs[0].address)
        assertEquals((txValue - fee), transaction.outputs[0].value)
    }

    @Test
    fun buildTransaction_InputsSigned() {
        val transaction = transactionBuilder.buildTransaction(txValue, toAddressP2PKH, feeRate, false)

        assertEquals(unlockingScript.toHexString(), transaction.inputs[0].sigScript.toHexString())
    }

    @Test
    fun fee() {
        val unspentOutputs = SelectedUnspentOutputInfo(listOf(), 11_805_400, 112_800, false)
        whenever(unspentOutputSelector.select(any(), any(), any(), any(), any(), any())).thenReturn(unspentOutputs)
        val fee = transactionBuilder.fee(10_782_000, 600, true)

        assertEquals(112800, fee)
    }

}

