package io.horizontalsystems.bitcoincore.managers

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import org.junit.Assert
import org.junit.jupiter.api.assertThrows
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object UnspentOutputSelectorSingleNoChangeTest : Spek({
    describe("#select") {
        val transactionSizeCalculator = mock<TransactionSizeCalculator>()
        val unspentOutputProvider = mock<IUnspentOutputProvider>()
        val unspentOutputSelector = UnspentOutputSelectorSingleNoChange(transactionSizeCalculator, unspentOutputProvider)

        context("when sending amount is dust") {
            it("it throws exception") {
                assertThrows<SendValueErrors.Dust> {
                    unspentOutputSelector.select(100, 1, ScriptType.P2PKH, ScriptType.P2PKH, true, 100, 0)
                }
            }
        }

        context("when there is no spendable utxo") {
            beforeEach {
                whenever(unspentOutputProvider.getSpendableUtxo()).thenReturn(listOf())
            }

            it("it throws exception") {
                assertThrows<SendValueErrors.EmptyOutputs> {
                    unspentOutputSelector.select(200, 1, ScriptType.P2PKH, ScriptType.P2PKH, true, 100, 0)
                }
            }
        }

        context("when there is no output that can be spent without change") {
            val feeRate = 1
            val transactionSize = 100L
            val outputValue = 1000L
            val unspentOutput = mock<UnspentOutput>()
            val transactionOutput = mock<TransactionOutput>() {
                on { scriptType } doReturn mock()
                on { value } doReturn outputValue
            }

            beforeEach {
                whenever(unspentOutputProvider.getSpendableUtxo()).thenReturn(listOf(unspentOutput))
                whenever(unspentOutput.output).thenReturn(transactionOutput)
                whenever(transactionSizeCalculator.transactionSize(any(), any(), any())).thenReturn(transactionSize)
            }

            it("it throws exception") {
                assertThrows<SendValueErrors.NoSingleOutput> {
                    unspentOutputSelector.select(200, feeRate, ScriptType.P2PKH, ScriptType.P2PKH, true, 100, 0)
                }
            }

        }

        context("when there is at least one output failed to spend before") {
            val feeRate = 1
            val unspentOutput = mock<UnspentOutput>()
            val transactionOutput = mock<TransactionOutput>() {
                on { failedToSpend } doReturn true
            }

            beforeEach {
                whenever(unspentOutputProvider.getSpendableUtxo()).thenReturn(listOf(unspentOutput))
                whenever(unspentOutput.output).thenReturn(transactionOutput)
            }

            it("throws error HasOutputFailedToSpend") {
                assertThrows<SendValueErrors.HasOutputFailedToSpend> {
                    unspentOutputSelector.select(200, feeRate, ScriptType.P2PKH, ScriptType.P2PKH, true, 100, 0)
                }
            }
        }

        context("success") {
            val sendingAmount = 200L
            val feeRate = 1
            val transactionSize = 100L
            val outputValue = 390L
            val dust = 100

            val unspentOutput = mock<UnspentOutput> {
                val transactionOutput = mock<TransactionOutput> {
                    on { scriptType } doReturn mock()
                    on { value } doReturn outputValue
                }

                on { output } doReturn transactionOutput
            }

            beforeEach {
                whenever(unspentOutputProvider.getSpendableUtxo()).thenReturn(listOf(unspentOutput))
                whenever(transactionSizeCalculator.transactionSize(any(), any(), any())).thenReturn(transactionSize)
            }

            it("returns SelectedUnspentOutputInfo object") {
                val selectedUnspentOutputInfo = unspentOutputSelector.select(sendingAmount, feeRate, ScriptType.P2PKH, ScriptType.P2PKH, true, dust, 0)

                Assert.assertNull(selectedUnspentOutputInfo.changeValue)
                Assert.assertArrayEquals(arrayOf(unspentOutput), selectedUnspentOutputInfo.outputs.toTypedArray())
                Assert.assertEquals(sendingAmount, selectedUnspentOutputInfo.recipientValue)
            }
        }
    }
})
