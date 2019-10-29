package io.horizontalsystems.bitcoincore.managers

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.TransactionSizeCalculator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import org.junit.Assert
import org.mockito.Mockito
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object UnspentOutputSelectorSingleNoChangeTest : Spek({
    describe("#select") {
        val txSizeCalculator = Mockito.mock(TransactionSizeCalculator::class.java)
        val unspentOutputProvider = Mockito.mock(UnspentOutputProvider::class.java)
        val unspentOutputSelector = UnspentOutputSelectorSingleNoChange(txSizeCalculator, unspentOutputProvider)

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

        it("select_ExactlyValueReceiverPay") {
            fun check(value: Long, feeRate: Int, fee: Long, senderPay: Boolean, output: TransactionOutput) {
                try {
                    val selectedOutputs = unspentOutputSelector.select(value = value, feeRate = feeRate, senderPay = senderPay, dust =, pluginDataOutputSize = 0)

                    Assert.assertArrayEquals(arrayOf(output), selectedOutputs.outputs.map { it.output }.toTypedArray())
                    Assert.assertEquals(output.value, selectedOutputs.totalValue)
                    Assert.assertEquals(fee, selectedOutputs.fee)
                    Assert.assertEquals(false, selectedOutputs.addChangeOutput)

                } catch (e: Exception) {
                    Assert.fail("tail failed with error: ${e.message}")
                }
            }

            check(value = 4000, feeRate = 1, fee = 100, senderPay = false, output = unspentOutputs[2].output)      // exactly, without fee
            check(value = 4000 - 5, feeRate = 1, fee = 100, senderPay = false, output = unspentOutputs[2].output)  // in range using dust, without fee
            check(value = 3900, feeRate = 1, fee = 100, senderPay = true, output = unspentOutputs[2].output)       // exactly, with fee
            check(value = 3900 - 5, feeRate = 1, fee = 105, senderPay = true, output = unspentOutputs[2].output)   // in range using dust, with fee
        }
    }
})
