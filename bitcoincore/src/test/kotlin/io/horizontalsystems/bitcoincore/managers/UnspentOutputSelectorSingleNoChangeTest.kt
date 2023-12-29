package io.horizontalsystems.bitcoincore.managers

import com.nhaarman.mockitokotlin2.any
import io.horizontalsystems.bitcoincore.DustCalculator
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class UnspentOutputSelectorSingleNoChangeTest {

    private val calculator: TransactionSizeCalculator = mock(TransactionSizeCalculator::class.java)
    private val dustCalculator: DustCalculator = mock(DustCalculator::class.java)
    private val unspentOutputProvider: IUnspentOutputProvider =
        mock(IUnspentOutputProvider::class.java)

    private val selector =
        UnspentOutputSelectorSingleNoChange(calculator, dustCalculator, unspentOutputProvider)
    private val dust = 100

    @Test
    fun testSelect_DustValue() {
        `when`(dustCalculator.dust(any())).thenReturn(dust)

        assertThrows(SendValueErrors.Dust::class.java) {
            selector.select(100, 1, ScriptType.P2PKH, ScriptType.P2PKH, true, 0)
        }
    }

    @Test
    fun testSelect_EmptyOutputs() {
        `when`(unspentOutputProvider.getSpendableUtxo()).thenReturn(emptyList())

        assertThrows(SendValueErrors.EmptyOutputs::class.java) {
            selector.select(10000, 100, ScriptType.P2PKH, ScriptType.P2WPKH, false, 0)
        }
    }

    // ... Add more tests for other scenarios, including successful selection and other error cases
}

//    describe("#select") {
//        val transactionSizeCalculator = mock<TransactionSizeCalculator>()
//        val unspentOutputProvider = mock<IUnspentOutputProvider>()
//        val dustCalculator = mock<DustCalculator>()
//        val unspentOutputSelector = UnspentOutputSelectorSingleNoChange(transactionSizeCalculator, dustCalculator, unspentOutputProvider)
//
//        context("when sending amount is dust") {
//            it("it throws exception") {
//                assertThrows<SendValueErrors.Dust> {
//                    unspentOutputSelector.select(100, 1, ScriptType.P2PKH, ScriptType.P2PKH, true, 100, 0)
//                }
//            }
//        }
//
//        context("when there is no spendable utxo") {
//            beforeEach {
//                whenever(unspentOutputProvider.getSpendableUtxo()).thenReturn(listOf())
//            }
//
//            it("it throws exception") {
//                assertThrows<SendValueErrors.EmptyOutputs> {
//                    unspentOutputSelector.select(200, 1, ScriptType.P2PKH, ScriptType.P2PKH, true, 100, 0)
//                }
//            }
//        }
//
//        context("when there is no output that can be spent without change") {
//            val feeRate = 1
//            val transactionSize = 100L
//            val outputValue = 1000L
//            val unspentOutput = mock<UnspentOutput>()
//            val transactionOutput = mock<TransactionOutput>() {
//                on { scriptType } doReturn mock()
//                on { value } doReturn outputValue
//            }
//
//            beforeEach {
//                whenever(unspentOutputProvider.getSpendableUtxo()).thenReturn(listOf(unspentOutput))
//                whenever(unspentOutput.output).thenReturn(transactionOutput)
//                whenever(transactionSizeCalculator.transactionSize(any(), any(), any())).thenReturn(transactionSize)
//            }
//
//            it("it throws exception") {
//                assertThrows<SendValueErrors.NoSingleOutput> {
//                    unspentOutputSelector.select(200, feeRate, ScriptType.P2PKH, ScriptType.P2PKH, true, 100, 0)
//                }
//            }
//
//        }
//
//        context("when there is at least one output failed to spend before") {
//            val feeRate = 1
//            val unspentOutput = mock<UnspentOutput>()
//            val transactionOutput = mock<TransactionOutput>() {
//                on { failedToSpend } doReturn true
//            }
//
//            beforeEach {
//                whenever(unspentOutputProvider.getSpendableUtxo()).thenReturn(listOf(unspentOutput))
//                whenever(unspentOutput.output).thenReturn(transactionOutput)
//            }
//
//            it("throws error HasOutputFailedToSpend") {
//                assertThrows<SendValueErrors.HasOutputFailedToSpend> {
//                    unspentOutputSelector.select(200, feeRate, ScriptType.P2PKH, ScriptType.P2PKH, true, 100, 0)
//                }
//            }
//        }
//
//        context("success") {
//            val sendingAmount = 200L
//            val feeRate = 1
//            val transactionSize = 100L
//            val outputValue = 390L
//            val dust = 100
//
//            val unspentOutput = mock<UnspentOutput> {
//                val transactionOutput = mock<TransactionOutput> {
//                    on { scriptType } doReturn mock()
//                    on { value } doReturn outputValue
//                }
//
//                on { output } doReturn transactionOutput
//            }
//
//            beforeEach {
//                whenever(unspentOutputProvider.getSpendableUtxo()).thenReturn(listOf(unspentOutput))
//                whenever(transactionSizeCalculator.transactionSize(any(), any(), any())).thenReturn(transactionSize)
//            }
//
//            it("returns SelectedUnspentOutputInfo object") {
//                val selectedUnspentOutputInfo = unspentOutputSelector.select(sendingAmount, feeRate, ScriptType.P2PKH, ScriptType.P2PKH, true, dust, 0)
//
//                Assert.assertNull(selectedUnspentOutputInfo.changeValue)
//                Assert.assertArrayEquals(arrayOf(unspentOutput), selectedUnspentOutputInfo.outputs.toTypedArray())
//                Assert.assertEquals(sendingAmount, selectedUnspentOutputInfo.recipientValue)
//            }
//        }
//    }
