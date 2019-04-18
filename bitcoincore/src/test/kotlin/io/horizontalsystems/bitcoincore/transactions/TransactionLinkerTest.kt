package io.horizontalsystems.bitcoincore.transactions

import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mockito.Mockito.mock
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class TransactionLinkerTest : Spek({
    val fullTransaction = mock(FullTransaction::class.java)
    val transactionOutput = mock(TransactionOutput::class.java)
    val transactionInput = mock(TransactionInput::class.java)
    val publicKey = mock(PublicKey::class.java)
    val storage = mock(IStorage::class.java)


    val linker by memoized { TransactionLinker(storage) }
    val transaction by memoized { Transaction() }

    beforeEachTest {
        whenever(fullTransaction.inputs).thenReturn(listOf(transactionInput))
        whenever(fullTransaction.header).thenReturn(transaction)
    }

    describe("has previous output") {

        beforeEach {
            whenever(storage.getPreviousOutput(transactionInput)).thenReturn(transactionOutput)
        }

        context("which is mine") {
            beforeEach {
                whenever(transactionOutput.publicKey(storage)).thenReturn(publicKey)
            }

            it("marks transaction as mine") {
                assertFalse(transaction.isMine)

                linker.handle(fullTransaction)

                assertTrue(transaction.isMine)
                assertTrue(transaction.isOutgoing)
            }
        }

        context("which is not mine") {
            beforeEach {
                whenever(transactionOutput.publicKey(storage)).thenReturn(null)
            }

            it("do not marks transaction as mine") {
                linker.handle(fullTransaction)
                assertFalse(transaction.isMine)
                assertFalse(transaction.isOutgoing)
            }
        }
    }

    describe("has not previous output") {
        beforeEach {
            whenever(storage.getPreviousOutput(transactionInput)).thenReturn(null)
        }

        it("do not marks transaction as mine") {
            linker.handle(fullTransaction)

            assertFalse(transaction.isMine)
            assertFalse(transaction.isOutgoing)
        }
    }
})
