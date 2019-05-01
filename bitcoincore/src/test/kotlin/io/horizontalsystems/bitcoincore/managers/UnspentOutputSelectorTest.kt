package io.horizontalsystems.bitcoincore.managers

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class UnspentOutputSelectorTest {
    private val txSizeCalculator = mock(TransactionSizeCalculator::class.java)
    private val unspentOutputProvider = mock(UnspentOutputProvider::class.java)
    private val unspentOutputSelector = UnspentOutputSelector(txSizeCalculator, unspentOutputProvider)

    private val publicKey = mock(PublicKey::class.java)
    private val transaction = mock(Transaction::class.java)
    private val block = mock(Block::class.java)

    private lateinit var unspentOutputs: List<UnspentOutput>

    @Before
    fun setUp() {
        val outputs = listOf(
                TransactionOutput().apply { value = 1000; scriptType = ScriptType.P2PKH },
                TransactionOutput().apply { value = 2000; scriptType = ScriptType.P2PKH },
                TransactionOutput().apply { value = 4000; scriptType = ScriptType.P2PKH },
                TransactionOutput().apply { value = 8000; scriptType = ScriptType.P2PKH },
                TransactionOutput().apply { value = 16000;scriptType = ScriptType.P2PKH })

        unspentOutputs = listOf(
                UnspentOutput(outputs[0], publicKey, transaction, block),
                UnspentOutput(outputs[1], publicKey, transaction, block),
                UnspentOutput(outputs[2], publicKey, transaction, block),
                UnspentOutput(outputs[3], publicKey, transaction, block),
                UnspentOutput(outputs[4], publicKey, transaction, block)
        )

        whenever(unspentOutputProvider.allUnspentOutputs()).thenReturn(unspentOutputs)
        whenever(txSizeCalculator.inputSize(any())).thenReturn(10)
        whenever(txSizeCalculator.outputSize(any())).thenReturn(2)
        whenever(txSizeCalculator.transactionSize(any(), any())).thenReturn(100)
    }

    @Test
    fun select_ExactlyValueReceiverPay() {
        fun check(value: Long, feeRate: Int, fee: Long, senderPay: Boolean, output: TransactionOutput) {
            try {
                val selectedOutputs = unspentOutputSelector.select(value = value, feeRate = feeRate, senderPay = senderPay)

                assertArrayEquals(arrayOf(output), selectedOutputs.outputs.map { it.output }.toTypedArray())
                assertEquals(output.value, selectedOutputs.totalValue)
                assertEquals(fee, selectedOutputs.fee)
                assertEquals(false, selectedOutputs.addChangeOutput)

            } catch (e: Exception) {
                fail("tail failed with error: ${e.message}")
            }
        }

        check(value = 4000, feeRate = 1, fee = 100, senderPay = false, output = unspentOutputs[2].output)      // exactly, without fee
        check(value = 4000 - 5, feeRate = 1, fee = 100, senderPay = false, output = unspentOutputs[2].output)  // in range using dust, without fee
        check(value = 3900, feeRate = 1, fee = 100, senderPay = true, output = unspentOutputs[2].output)       // exactly, with fee
        check(value = 3900 - 5, feeRate = 1, fee = 105, senderPay = true, output = unspentOutputs[2].output)   // in range using dust, with fee
    }

    @Test
    fun select_receiverPay() {
        val selectedOutput = unspentOutputSelector.select(value = 7000, feeRate = 1, senderPay = true)

        assertEquals(listOf(unspentOutputs[0], unspentOutputs[1], unspentOutputs[2], unspentOutputs[3]), selectedOutput.outputs)
        assertEquals(15000, selectedOutput.totalValue)
        assertEquals(100, selectedOutput.fee)
        assertEquals(true, selectedOutput.addChangeOutput)
    }

    @Test
    fun select_receiverPayNoChangeOutput() {
        val expectedFee = (100 + 10 + 2).toLong()  // fee for tx + fee for change input + fee for change output
        val selectedOutputs = unspentOutputSelector.select(value = 15000L - expectedFee, feeRate = 1, senderPay = true)

        assertEquals(listOf(unspentOutputs[0], unspentOutputs[1], unspentOutputs[2], unspentOutputs[3]), selectedOutputs.outputs)
        assertEquals(15000, selectedOutputs.totalValue)
        assertEquals(expectedFee, selectedOutputs.fee)
        assertEquals(false, selectedOutputs.addChangeOutput)
    }

    @Test(expected = UnspentOutputSelector.Error.InsufficientUnspentOutputs::class)
    fun testNotEnoughErrorReceiverPay() {
        unspentOutputSelector.select(value = 3_100_100, feeRate = 600, outputType = ScriptType.P2PKH, senderPay = false)
    }

    @Test(expected = UnspentOutputSelector.Error.EmptyUnspentOutputs::class)
    fun testEmptyOutputsError() {
        whenever(unspentOutputProvider.allUnspentOutputs()).thenReturn(listOf())
        unspentOutputSelector.select(value = 3_090_000, feeRate = 600, outputType = ScriptType.P2PKH, senderPay = true)
    }

}
