package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType.P2PK
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType.P2PKH
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType.P2SH
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType.P2WPKH
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType.P2WPKHSH
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionSizeCalculatorTest {
    private val calculator = TransactionSizeCalculator()

    @Test
    fun transactionSize() {
        assertEquals(10, calculator.transactionSize(listOf(), listOf()))
        assertEquals(194, calculator.transactionSize(listOf(P2PKH), listOf(P2PKH)))
        assertEquals(310, calculator.transactionSize(listOf(P2PKH, P2PK), listOf(P2PKH)))
        assertEquals(307, calculator.transactionSize(listOf(P2PKH, P2PK), listOf(P2WPKH)))      // 2-in 1-out legacy tx with witness output
        assertEquals(354, calculator.transactionSize(listOf(P2PKH, P2PK), listOf(P2PKH, P2PK))) // 2-in 2-out legacy tx

        assertEquals(113, calculator.transactionSize(listOf(P2WPKH), listOf(P2PKH)))        // 1-in 1-out witness tx
        assertEquals(136, calculator.transactionSize(listOf(P2WPKHSH), listOf(P2PKH)))      // 1-in 1-out (sh) witness tx
        assertEquals(263, calculator.transactionSize(listOf(P2WPKH, P2PKH), listOf(P2PKH))) // 2-in 1-out witness tx
    }

    @Test
    fun inputSize() {
        assertEquals(150, calculator.inputSize(P2PKH))
        assertEquals(116, calculator.inputSize(P2PK))
        assertEquals(41, calculator.inputSize(P2WPKH))
        assertEquals(64, calculator.inputSize(P2WPKHSH))
    }

    @Test
    fun outputSize() {
        assertEquals(34, calculator.outputSize(P2PKH))
        assertEquals(32, calculator.outputSize(P2SH))
        assertEquals(44, calculator.outputSize(P2PK))
        assertEquals(31, calculator.outputSize(P2WPKH))
        assertEquals(32, calculator.outputSize(P2WPKHSH))
    }
}
