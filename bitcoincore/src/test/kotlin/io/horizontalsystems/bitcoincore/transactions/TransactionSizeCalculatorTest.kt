package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType.*
import org.junit.Assert.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TransactionSizeCalculatorTest : Spek({
    val calculator = TransactionSizeCalculator()

    describe("calculate size") {

        fun outputs(scriptTypes: List<ScriptType>): List<TransactionOutput> {
            return scriptTypes.map { TransactionOutput().apply { this.scriptType = it } }
        }

        it("transactionSize") {
            assertEquals(10, calculator.transactionSize(listOf(), listOf(), 0))
            assertEquals(192, calculator.transactionSize(outputs(listOf(P2PKH)), listOf(P2PKH), 0))
            assertEquals(306, calculator.transactionSize(outputs(listOf(P2PKH, P2PK)), listOf(P2PKH), 0))
            assertEquals(303, calculator.transactionSize(outputs(listOf(P2PKH, P2PK)), listOf(P2WPKH), 0))      // 2-in 1-out legacy tx with witness output
            assertEquals(350, calculator.transactionSize(outputs(listOf(P2PKH, P2PK)), listOf(P2PKH, P2PK), 0)) // 2-in 2-out legacy tx

            assertEquals(113, calculator.transactionSize(outputs(listOf(P2WPKH)), listOf(P2PKH), 0))        // 1-in 1-out witness tx
            assertEquals(136, calculator.transactionSize(outputs(listOf(P2WPKHSH)), listOf(P2PKH), 0))      // 1-in 1-out (sh) witness tx
            assertEquals(261, calculator.transactionSize(outputs(listOf(P2WPKH, P2PKH)), listOf(P2PKH), 0)) // 2-in 1-out witness tx
        }

        it("inputSize") {
            assertEquals(148, calculator.inputSize(P2PKH))
            assertEquals(114, calculator.inputSize(P2PK))
            assertEquals(41, calculator.inputSize(P2WPKH))
            assertEquals(64, calculator.inputSize(P2WPKHSH))
        }

        it("outputSize") {
            assertEquals(34, calculator.outputSize(P2PKH))
            assertEquals(32, calculator.outputSize(P2SH))
            assertEquals(44, calculator.outputSize(P2PK))
            assertEquals(31, calculator.outputSize(P2WPKH))
            assertEquals(32, calculator.outputSize(P2WPKHSH))
        }

    }
})
