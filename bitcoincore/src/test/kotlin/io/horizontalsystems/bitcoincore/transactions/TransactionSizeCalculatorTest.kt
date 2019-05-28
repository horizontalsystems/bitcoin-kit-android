package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType.P2PK
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType.P2PKH
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType.P2SH
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType.P2WPKH
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType.P2WPKHSH
import org.junit.Assert.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TransactionSizeCalculatorTest : Spek({
    val calculator = TransactionSizeCalculator()

    describe("calculate size") {

        it("transactionSize") {
            assertEquals(10, calculator.transactionSize(listOf(), listOf()))
            assertEquals(194, calculator.transactionSize(listOf(P2PKH), listOf(P2PKH)))
            assertEquals(310, calculator.transactionSize(listOf(P2PKH, P2PK), listOf(P2PKH)))
            assertEquals(307, calculator.transactionSize(listOf(P2PKH, P2PK), listOf(P2WPKH)))      // 2-in 1-out legacy tx with witness output
            assertEquals(354, calculator.transactionSize(listOf(P2PKH, P2PK), listOf(P2PKH, P2PK))) // 2-in 2-out legacy tx

            assertEquals(113, calculator.transactionSize(listOf(P2WPKH), listOf(P2PKH)))        // 1-in 1-out witness tx
            assertEquals(136, calculator.transactionSize(listOf(P2WPKHSH), listOf(P2PKH)))      // 1-in 1-out (sh) witness tx
            assertEquals(263, calculator.transactionSize(listOf(P2WPKH, P2PKH), listOf(P2PKH))) // 2-in 1-out witness tx
        }

        it("inputSize") {
            assertEquals(150, calculator.inputSize(P2PKH))
            assertEquals(116, calculator.inputSize(P2PK))
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
