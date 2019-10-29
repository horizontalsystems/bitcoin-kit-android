package io.horizontalsystems.bitcoincore.managers

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.Fixtures
import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import org.junit.Assert
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object UnspentOutputSelectorTest : Spek({

    describe("#select") {
        context("when there is no limit for outputs") {
            val txSizeCalculator = Mockito.mock(TransactionSizeCalculator::class.java)
            val unspentOutputProvider = Mockito.mock(UnspentOutputProvider::class.java)
            val unspentOutputSelector = UnspentOutputSelector(txSizeCalculator, unspentOutputProvider)

            val publicKey = Mockito.mock(PublicKey::class.java)
            val transaction = Mockito.mock(Transaction::class.java)
            val block = Mockito.mock(Block::class.java)

            lateinit var unspentOutputs: List<UnspentOutput>

            beforeEach {
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

                whenever(unspentOutputProvider.getSpendableUtxo()).thenReturn(unspentOutputs)
                whenever(txSizeCalculator.inputSize(any())).thenReturn(10)
                whenever(txSizeCalculator.outputSize(any())).thenReturn(2)
                whenever(txSizeCalculator.transactionSize(any(), any(), any())).thenReturn(100)
            }

            it("select_receiverPay") {
                val selectedOutput = unspentOutputSelector.select(value = 7000, feeRate = 1, senderPay = true, dust =, pluginDataOutputSize = 0)

                Assert.assertEquals(listOf(unspentOutputs[0], unspentOutputs[1], unspentOutputs[2], unspentOutputs[3]), selectedOutput.outputs)
                Assert.assertEquals(15000, selectedOutput.totalValue)
                Assert.assertEquals(100, selectedOutput.fee)
                Assert.assertEquals(true, selectedOutput.addChangeOutput)
            }

            it("select_receiverPayNoChangeOutput") {
                val expectedFee = (100 + 10 + 2).toLong()  // fee for tx + fee for change input + fee for change output
                val selectedOutputs = unspentOutputSelector.select(value = 15000L - expectedFee, feeRate = 1, senderPay = true, dust =, pluginDataOutputSize = 0)

                Assert.assertEquals(listOf(unspentOutputs[0], unspentOutputs[1], unspentOutputs[2], unspentOutputs[3]), selectedOutputs.outputs)
                Assert.assertEquals(15000, selectedOutputs.totalValue)
                Assert.assertEquals(expectedFee, selectedOutputs.fee)
                Assert.assertEquals(false, selectedOutputs.addChangeOutput)
            }

            it("testNotEnoughErrorReceiverPay") {
                assertThrows<SendValueErrors.InsufficientUnspentOutputs> {
                    unspentOutputSelector.select(value = 3_100_100, feeRate = 600, outputType = ScriptType.P2PKH, senderPay = false, dust =, pluginDataOutputSize = 0)
                }
            }

            it("testEmptyOutputsError") {
                whenever(unspentOutputProvider.getSpendableUtxo()).thenReturn(listOf())

                assertThrows<SendValueErrors.EmptyOutputs> {
                    unspentOutputSelector.select(value = 3_090_000, feeRate = 600, outputType = ScriptType.P2PKH, senderPay = true, dust =, pluginDataOutputSize = 0)
                }
            }

        }

        context("when there is a limit for 4 outputs") {
            val calculator = mock<TransactionSizeCalculator>()
            val unspentOutputProvider = mock<IUnspentOutputProvider>()
            val selector by memoized { UnspentOutputSelector(calculator, unspentOutputProvider, 4) }

            val feeRate = 0
            val utxo1 = Fixtures.unspentOutput(100L)
            val utxo2 = Fixtures.unspentOutput(200L)
            val utxo3 = Fixtures.unspentOutput(300L)
            val utxo4 = Fixtures.unspentOutput(400L)
            val utxo5 = Fixtures.unspentOutput(500L)

            val unspentOutputs = listOf(utxo1, utxo2, utxo3, utxo4, utxo5)

            beforeEach {
                whenever(unspentOutputProvider.getSpendableUtxo()).thenReturn(unspentOutputs)
                whenever(calculator.transactionSize(any(), any(), any())).thenReturn(123123)
            }

            it("selects selects consecutive 4 outputs") {
                Assertions.assertArrayEquals(arrayOf(utxo1, utxo2, utxo3, utxo4), selector.select(1000, feeRate, senderPay = true, dust =, pluginDataOutputSize = 0).outputs.toTypedArray())
                Assertions.assertArrayEquals(arrayOf(utxo2, utxo3, utxo4, utxo5), selector.select(1100, feeRate, senderPay = true, dust =, pluginDataOutputSize = 0).outputs.toTypedArray())
            }
        }
    }
})
