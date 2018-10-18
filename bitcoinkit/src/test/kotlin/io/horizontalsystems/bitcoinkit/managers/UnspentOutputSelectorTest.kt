package io.horizontalsystems.bitcoinkit.managers

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.models.TransactionOutput
import io.horizontalsystems.bitcoinkit.scripts.ScriptType
import io.horizontalsystems.bitcoinkit.transactions.TransactionSizeCalculator
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class UnspentOutputSelectorTest {

    private val txSizeCalculator = mock(TransactionSizeCalculator::class.java)
    private val unspentOutputSelector = UnspentOutputSelector(txSizeCalculator)
    private lateinit var outputs: List<TransactionOutput>

    @Before
    fun setUp() {
        outputs = listOf(TransactionOutput().apply {
            value = 100_000
            scriptType = ScriptType.P2PKH
        }, TransactionOutput().apply {
            value = 200_000
            scriptType = ScriptType.P2PKH
        }, TransactionOutput().apply {
            value = 400_000
            scriptType = ScriptType.P2PKH
        }, TransactionOutput().apply {
            value = 800_000
            scriptType = ScriptType.P2PKH
        }, TransactionOutput().apply {
            value = 1_600_000
            scriptType = ScriptType.P2PKH
        })

        whenever(txSizeCalculator.inputSize(any())).thenReturn(149)
        whenever(txSizeCalculator.outputSize(any())).thenReturn(34)
        whenever(txSizeCalculator.emptyTxSize).thenReturn(10)
    }

    @After
    fun tearDown() {
    }

    @Test
    fun testExactlyValueReceiverPay() {
        val selectedOutputs = unspentOutputSelector.select(value = 400_000, feeRate = 600, outputScriptType = ScriptType.P2PKH, senderPay = false, unspentOutputs = outputs)

        Assert.assertEquals(listOf(outputs[2]), selectedOutputs.outputs)
        Assert.assertEquals(400_000, selectedOutputs.totalValue)
        Assert.assertEquals(115_800, selectedOutputs.fee)
    }

    @Test
    fun testExactlyValueSenderPay() {
        val fee = (10 + 149 + 29) * 600 // transaction + 1 input + 1 output
        val selectedOutputs = unspentOutputSelector.select(value = 339_950 - fee, feeRate = 600, outputScriptType = ScriptType.P2PKH, senderPay = true, unspentOutputs = outputs)

        Assert.assertEquals(listOf(outputs[2]), selectedOutputs.outputs)
        Assert.assertEquals(400_000, selectedOutputs.totalValue)
        Assert.assertEquals(115_800, selectedOutputs.fee)
    }

    @Test
    fun testTotalValueReceiverPay() {
        val selectedOutputs = unspentOutputSelector.select(value = 700_000, feeRate = 600, outputScriptType = ScriptType.P2PKH, senderPay = false, unspentOutputs = outputs)

        Assert.assertEquals(listOf(outputs[0], outputs[1], outputs[2]), selectedOutputs.outputs)
        Assert.assertEquals(700_000, selectedOutputs.totalValue)
        Assert.assertEquals(294_600, selectedOutputs.fee)
    }

    @Test
    fun testTotalValueSenderPay() {
        val selectedOutputs = unspentOutputSelector.select(value = 700_000, feeRate = 600, outputScriptType = ScriptType.P2PKH, senderPay = true, unspentOutputs = outputs)

        Assert.assertEquals(listOf(outputs[0], outputs[1], outputs[2], outputs[3]), selectedOutputs.outputs)
        Assert.assertEquals(1_500_000, selectedOutputs.totalValue)
        Assert.assertEquals(384_000, selectedOutputs.fee)
    }

    @Test(expected = UnspentOutputSelector.InsufficientUnspentOutputs::class)
    fun testNotEnoughErrorReceiverPay() {
        unspentOutputSelector.select(value = 3_100_100, feeRate = 600, outputScriptType = ScriptType.P2PKH, senderPay = false, unspentOutputs = outputs)
    }

    @Test(expected = UnspentOutputSelector.InsufficientUnspentOutputs::class)
    fun testNotEnoughErrorSenderPay() {
        unspentOutputSelector.select(value = 3_090_000, feeRate = 600, outputScriptType = ScriptType.P2PKH, senderPay = true, unspentOutputs = outputs)
    }

    @Test(expected = UnspentOutputSelector.EmptyUnspentOutputs::class)
    fun testEmptyOutputsError() {
        unspentOutputSelector.select(value = 3_090_000, feeRate = 600, outputScriptType = ScriptType.P2PKH, senderPay = true, unspentOutputs = listOf())
    }

}
