package io.horizontalsystems.bitcoincore.managers

import com.nhaarman.mockitokotlin2.any
import io.horizontalsystems.bitcoincore.DustCalculator
import io.horizontalsystems.bitcoincore.Fixtures
import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`


class UnspentOutputSelectorTest {

    private val calculator: TransactionSizeCalculator = mock(TransactionSizeCalculator::class.java)
    private val dustCalculator: DustCalculator = mock(DustCalculator::class.java)
    private val unspentOutputProvider: IUnspentOutputProvider = mock(IUnspentOutputProvider::class.java)
    private val unspentOutputQueue: UnspentOutputQueue = mock(UnspentOutputQueue::class.java)
    private val dust = 100

//    @Before
//    fun setup() {
//        unspentOutputQueue = UnspentOutputQueue(
//            UnspentOutputQueue.Parameters(
//                value = 0,
//                senderPay = false,
//                fee = 0,
//                outputsLimit = null,
//                outputScriptType = ScriptType.P2PKH,
//                changeType = ScriptType.P2PKH,
//                pluginDataOutputSize = 0
//            ),
//            calculator,
//            dustCalculator
//        )
//    }

    private val selector = UnspentOutputSelector(calculator, dustCalculator, unspentOutputProvider)

    @Test
    fun testSelect_DustValue() {
        val value = 54L
        val dust = 100
        `when`(dustCalculator.dust(any())).thenReturn(dust)

        assertThrows(SendValueErrors.Dust::class.java) {
            selector.select(value, 100, ScriptType.P2PKH, ScriptType.P2WPKH, false, 0)
        }
    }

    @Test
    fun testSelect_EmptyOutputs() {
        `when`(unspentOutputProvider.getSpendableUtxo()).thenReturn(emptyList())

        assertThrows(SendValueErrors.InsufficientUnspentOutputs::class.java) {
            selector.select(10000, 100, ScriptType.P2PKH, ScriptType.P2WPKH, false, 0)
        }
    }

    @Test
    fun testSelect_SuccessfulSelection() {
        val outputs = listOf(
            createUnspentOutput(10000),
            createUnspentOutput(5000)
        )
//        unspentOutputQueue.push(outputs[0])
//        unspentOutputQueue.push(outputs[1])

        `when`(unspentOutputProvider.getSpendableUtxo()).thenReturn(outputs)
        `when`(dustCalculator.dust(any())).thenReturn(dust)
        `when`(calculator.inputSize(any())).thenReturn(10)
        `when`(calculator.outputSize(any())).thenReturn(2)
        `when`(calculator.transactionSize(anyList(), anyList(), any())).thenReturn(150)
        `when`(unspentOutputQueue.calculate()).thenReturn(SelectedUnspentOutputInfo(outputs, 12000, 0))

        val selectedInfo = selector.select(12000, 100, ScriptType.P2PKH, ScriptType.P2WPKH, false, 0)
        assertEquals(outputs, selectedInfo.outputs)
        assertEquals(12000, selectedInfo.recipientValue)
    }

    private fun createUnspentOutput(value: Long): UnspentOutput {
        val output =
            TransactionOutput(value = value, index = 0, script = byteArrayOf(), type = ScriptType.P2PKH, lockingScriptPayload = "000010000".hexToByteArray())
        val pubKey = Fixtures.publicKey
        val transaction = mock(Transaction::class.java)
        val block = mock(Block::class.java)

        return UnspentOutput(output, pubKey, transaction, block)
    }
}

//    describe("#select") {
//        context("when there is no limit for outputs") {
//            val txSizeCalculator = Mockito.mock(TransactionSizeCalculator::class.java)
//            val unspentOutputProvider = Mockito.mock(UnspentOutputProvider::class.java)
//            val unspentOutputSelector = UnspentOutputSelector(txSizeCalculator, unspentOutputProvider)
//
//            val publicKey = Mockito.mock(PublicKey::class.java)
//            val transaction = Mockito.mock(Transaction::class.java)
//            val block = Mockito.mock(Block::class.java)
//
//            lateinit var unspentOutputs: List<UnspentOutput>
//
//            beforeEach {
//                val outputs = listOf(
//                        TransactionOutput().apply { value = 1000; scriptType = ScriptType.P2PKH },
//                        TransactionOutput().apply { value = 2000; scriptType = ScriptType.P2PKH },
//                        TransactionOutput().apply { value = 4000; scriptType = ScriptType.P2PKH },
//                        TransactionOutput().apply { value = 8000; scriptType = ScriptType.P2PKH },
//                        TransactionOutput().apply { value = 16000;scriptType = ScriptType.P2PKH })
//
//                unspentOutputs = listOf(
//                        UnspentOutput(outputs[0], publicKey, transaction, block),
//                        UnspentOutput(outputs[1], publicKey, transaction, block),
//                        UnspentOutput(outputs[2], publicKey, transaction, block),
//                        UnspentOutput(outputs[3], publicKey, transaction, block),
//                        UnspentOutput(outputs[4], publicKey, transaction, block)
//                )
//
//                whenever(unspentOutputProvider.getSpendableUtxo()).thenReturn(unspentOutputs)
//                whenever(txSizeCalculator.inputSize(any())).thenReturn(10)
//                whenever(txSizeCalculator.outputSize(any())).thenReturn(2)
//                whenever(txSizeCalculator.transactionSize(any(), any(), any())).thenReturn(100)
//            }
//
//            it("select_receiverPay") {
//                val selectedOutput = unspentOutputSelector.select(value = 7000, feeRate = 1, senderPay = true, dust = 1, pluginDataOutputSize = 0)
//
//                Assert.assertEquals(listOf(unspentOutputs[0], unspentOutputs[1], unspentOutputs[2], unspentOutputs[3]), selectedOutput.outputs)
//                Assert.assertEquals(7000, selectedOutput.recipientValue)
//                Assert.assertEquals(8000 - 100L, selectedOutput.changeValue)
//            }
//
//            it("testNotEnoughErrorReceiverPay") {
//                assertThrows<SendValueErrors.InsufficientUnspentOutputs> {
//                    unspentOutputSelector.select(value = 3_100_100, feeRate = 600, outputType = ScriptType.P2PKH, senderPay = false, dust = 1, pluginDataOutputSize = 0)
//                }
//            }
//
//            it("testEmptyOutputsError") {
//                whenever(unspentOutputProvider.getSpendableUtxo()).thenReturn(listOf())
//
//                assertThrows<SendValueErrors.EmptyOutputs> {
//                    unspentOutputSelector.select(value = 3_090_000, feeRate = 600, outputType = ScriptType.P2PKH, senderPay = true, dust = 1, pluginDataOutputSize = 0)
//                }
//            }
//
//        }
//
//        context("when there is a limit for 4 outputs") {
//            val calculator = mock<TransactionSizeCalculator>()
//            val unspentOutputProvider = mock<IUnspentOutputProvider>()
//            val selector by memoized { UnspentOutputSelector(calculator, unspentOutputProvider, 4) }
//
//            val feeRate = 0
//            val utxo1 = Fixtures.unspentOutput(100L)
//            val utxo2 = Fixtures.unspentOutput(200L)
//            val utxo3 = Fixtures.unspentOutput(300L)
//            val utxo4 = Fixtures.unspentOutput(400L)
//            val utxo5 = Fixtures.unspentOutput(500L)
//
//            val unspentOutputs = listOf(utxo1, utxo2, utxo3, utxo4, utxo5)
//
//            beforeEach {
//                whenever(unspentOutputProvider.getSpendableUtxo()).thenReturn(unspentOutputs)
//                whenever(calculator.transactionSize(any(), any(), any())).thenReturn(123123)
//            }
//
//            it("selects selects consecutive 4 outputs") {
//                Assertions.assertArrayEquals(arrayOf(utxo1, utxo2, utxo3, utxo4), selector.select(1000, feeRate, senderPay = true, dust = 1, pluginDataOutputSize = 0).outputs.toTypedArray())
//                Assertions.assertArrayEquals(arrayOf(utxo2, utxo3, utxo4, utxo5), selector.select(1100, feeRate, senderPay = true, dust = 1, pluginDataOutputSize = 0).outputs.toTypedArray())
//            }
//        }
//
//        context("when there are outputs with the failed status") {
//            val txSizeCalculator = Mockito.mock(TransactionSizeCalculator::class.java)
//            val unspentOutputProvider = Mockito.mock(UnspentOutputProvider::class.java)
//            val selector = UnspentOutputSelector(txSizeCalculator, unspentOutputProvider)
//
//            val publicKey = Mockito.mock(PublicKey::class.java)
//            val transaction = Mockito.mock(Transaction::class.java)
//            val block = Mockito.mock(Block::class.java)
//
//            val outputs = listOf(
//                    TransactionOutput().apply { value = 1000; failedToSpend = false },
//                    TransactionOutput().apply { value = 1000; failedToSpend = false },
//                    TransactionOutput().apply { value = 2000; failedToSpend = true },
//                    TransactionOutput().apply { value = 2000; failedToSpend = false })
//
//            val unspentOutputFailed = UnspentOutput(outputs[2], publicKey, transaction, block)
//            val unspentOutputs = listOf(
//                    UnspentOutput(outputs[0], publicKey, transaction, block),
//                    UnspentOutput(outputs[1], publicKey, transaction, block),
//                    unspentOutputFailed,
//                    UnspentOutput(outputs[3], publicKey, transaction, block)
//            )
//
//            beforeEach {
//                whenever(unspentOutputProvider.getSpendableUtxo()).thenReturn(unspentOutputs)
//                whenever(txSizeCalculator.inputSize(any())).thenReturn(10)
//                whenever(txSizeCalculator.outputSize(any())).thenReturn(2)
//                whenever(txSizeCalculator.transactionSize(any(), any(), any())).thenReturn(100)
//            }
//
//
//            it("first selects the failed ones") {
//                val unspentOutputInfo = selector.select(100, 1, senderPay = true, dust = 1, pluginDataOutputSize = 0)
//
//                Assert.assertEquals(listOf(unspentOutputFailed), unspentOutputInfo.outputs)
//            }
//        }
//    }

