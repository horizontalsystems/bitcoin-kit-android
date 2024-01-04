package io.horizontalsystems.bitcoincore.managers

import com.nhaarman.mockitokotlin2.any
import io.horizontalsystems.bitcoincore.DustCalculator
import io.horizontalsystems.bitcoincore.Fixtures
import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import org.junit.Assert
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class UnspentOutputSelectorSingleNoChangeTest {

    private val calculator: TransactionSizeCalculator = mock(TransactionSizeCalculator::class.java)
    private val dustCalculator: DustCalculator = mock(DustCalculator::class.java)
    private val unspentOutputProvider: IUnspentOutputProvider =
        mock(IUnspentOutputProvider::class.java)
    private val queueParams: UnspentOutputQueue.Parameters =
        mock(UnspentOutputQueue.Parameters::class.java)

    private val dust = 100

    @Test
    fun testSelect_DustValue() {
        val value = 54L
        val selector =
            UnspentOutputSelectorSingleNoChange(calculator, dustCalculator, unspentOutputProvider)
        `when`(dustCalculator.dust(any())).thenReturn(dust)

        assertThrows(SendValueErrors.Dust::class.java) {
            selector.select(value, 100, ScriptType.P2PKH, ScriptType.P2WPKH, false, 0)
        }
    }

    @Test
    fun testSelect_EmptyOutputs() {
        val selector =
            UnspentOutputSelectorSingleNoChange(calculator, dustCalculator, unspentOutputProvider)
        `when`(unspentOutputProvider.getSpendableUtxo()).thenReturn(emptyList())

        assertThrows(SendValueErrors.EmptyOutputs::class.java) {
            selector.select(10000, 100, ScriptType.P2PKH, ScriptType.P2WPKH, false, 0)
        }
    }

    @Test
    fun testSelect_NoSingleOutput() {
        val selector = UnspentOutputSelectorSingleNoChange(calculator, dustCalculator, unspentOutputProvider)
        val outputs = listOf(
            createUnspentOutput(5000),
            createUnspentOutput(10000)
        )

        val fee = 150
        val value = 6000L

        `when`(unspentOutputProvider.getSpendableUtxo()).thenReturn(outputs)
        `when`(dustCalculator.dust(any())).thenReturn(dust)
        `when`(calculator.inputSize(any())).thenReturn(10)
        `when`(calculator.outputSize(any())).thenReturn(2)
        `when`(calculator.transactionSize(
            ArgumentMatchers.anyList(),
            ArgumentMatchers.anyList(), any())).thenReturn(30)
        `when`(queueParams.value).thenReturn(value)
        `when`(queueParams.fee).thenReturn(fee)

        assertThrows(SendValueErrors.NoSingleOutput::class.java) {
            selector.select(value, 100, ScriptType.P2PKH, ScriptType.P2WPKH, false, 0)
        }
    }

    @Test
    fun testSelect_SingleOutputSuccess() {
        val selector = UnspentOutputSelectorSingleNoChange(calculator, dustCalculator, unspentOutputProvider)
        val outputs = listOf(
            createUnspentOutput(5000),
            createUnspentOutput(10000)
        )

        val feeRate = 5
        val fee = 150
        val value = 10000L

        `when`(unspentOutputProvider.getSpendableUtxo()).thenReturn(outputs)
        `when`(dustCalculator.dust(any())).thenReturn(dust)
        `when`(calculator.inputSize(any())).thenReturn(10)
        `when`(calculator.outputSize(any())).thenReturn(2)
        `when`(calculator.transactionSize(
            ArgumentMatchers.anyList(),
            ArgumentMatchers.anyList(), any())).thenReturn(30)
        `when`(queueParams.value).thenReturn(value)
        `when`(queueParams.fee).thenReturn(fee)

        val selectedInfo =
            selector.select(value, feeRate, ScriptType.P2PKH, ScriptType.P2WPKH, false, 0)
        Assert.assertEquals(null, selectedInfo.changeValue)
        Assert.assertEquals(1, selectedInfo.outputs.size)
        Assert.assertArrayEquals(arrayOf(outputs[1]), selectedInfo.outputs.toTypedArray())
    }

    @Test
    fun testSelect_HasOutputFailedToSpend() {
        val selector = UnspentOutputSelectorSingleNoChange(calculator, dustCalculator, unspentOutputProvider)
        val outputs = listOf(
            createUnspentOutput(5000),
            createUnspentOutput(10000, true)
        )

        val fee = 150
        val value = 10000L

        `when`(unspentOutputProvider.getSpendableUtxo()).thenReturn(outputs)
        `when`(dustCalculator.dust(any())).thenReturn(dust)
        `when`(calculator.inputSize(any())).thenReturn(10)
        `when`(calculator.outputSize(any())).thenReturn(2)
        `when`(calculator.transactionSize(
            ArgumentMatchers.anyList(),
            ArgumentMatchers.anyList(), any())).thenReturn(30)
        `when`(queueParams.value).thenReturn(value)
        `when`(queueParams.fee).thenReturn(fee)

        assertThrows(SendValueErrors.HasOutputFailedToSpend::class.java) {
            selector.select(value, 100, ScriptType.P2PKH, ScriptType.P2WPKH, false, 0)
        }
    }

    private fun createUnspentOutput(value: Long, failedToSpend: Boolean = false): UnspentOutput {
        val output =
            TransactionOutput(
                value = value,
                index = 0,
                script = byteArrayOf(),
                type = ScriptType.P2PKH,
                lockingScriptPayload = null
            )
        if (failedToSpend) {
            output.failedToSpend = true
        }
        val pubKey = Fixtures.publicKey
        val transaction = mock(Transaction::class.java)
        val block = mock(Block::class.java)

        return UnspentOutput(output, pubKey, transaction, block)
    }

}
