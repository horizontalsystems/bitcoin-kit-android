package io.horizontalsystems.bitcoincore.transactions

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoincore.Fixtures
import io.horizontalsystems.bitcoincore.transactions.builder.TransactionBuilder
import org.mockito.Mockito
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TransactionCreatorTest : Spek({

    val transactionBuilder = Mockito.mock(TransactionBuilder::class.java)
    val transactionProcessor = Mockito.mock(TransactionProcessor::class.java)
    val transactionSender = Mockito.mock(TransactionSender::class.java)
    val transactionP2PKH = Fixtures.transactionP2PKH
    val transactionCreator = TransactionCreator(transactionBuilder, transactionProcessor, transactionSender)

    beforeEachTest {
        whenever(transactionBuilder.buildTransaction(any(), any(), any(), any())).thenReturn(transactionP2PKH)
    }

    describe("#create") {
        it("success") {
            transactionCreator.create("address", 10_000_000, 8, true)

            verify(transactionProcessor).processOutgoing(transactionP2PKH)
        }
    }

})

