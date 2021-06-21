package io.horizontalsystems.bitcoincore.transactions

import com.nhaarman.mockitokotlin2.*
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.managers.BloomFilterManager
import io.horizontalsystems.bitcoincore.managers.PublicKeyManager
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TransactionSyncerTest : Spek({
    lateinit var syncer: TransactionSyncer

    val storage = mock(IStorage::class.java)
    val transactionProcessor = mock(PendingTransactionProcessor::class.java)
    val publicKeyManager = mock(PublicKeyManager::class.java)
    val invalidator = mock(TransactionInvalidator::class.java)

    val fullTransaction = mock(FullTransaction::class.java)
    val transaction = mock(Transaction::class.java)

    beforeEachTest {
        whenever(transaction.hash).thenReturn(byteArrayOf(1, 2, 3))
        whenever(fullTransaction.header).thenReturn(transaction)

        syncer = TransactionSyncer(storage, transactionProcessor, invalidator, publicKeyManager)
    }

    afterEachTest {
        reset(storage, transactionProcessor, publicKeyManager)
    }

    describe("handleTransactions") {
        context("when empty array is given") {
            it("doesn't do anything") {
                syncer.handleRelayed(listOf())

                verify(transactionProcessor, never()).processReceived(any(), any())
                verify(publicKeyManager, never()).fillGap()
            }
        }

        context("when not empty array is given") {
            val transactions = listOf(fullTransaction)

            context("when need to update bloom filter") {
                beforeEach {
                    whenever(transactionProcessor.processReceived(eq(transactions), eq(false)))
                            .thenThrow(BloomFilterManager.BloomFilterExpired)
                }

                it("fills addresses gap and regenerates bloom filter") {
                    syncer.handleRelayed(transactions)

                    verify(transactionProcessor).processReceived(eq(transactions), eq(false))
                    verify(publicKeyManager).fillGap()
                }
            }

            context("when don't need to update bloom filter") {
                it("doesn't run address fillGap and doesn't regenerate bloom filter") {
                    syncer.handleRelayed(transactions)

                    verify(transactionProcessor).processReceived(eq(transactions), eq(false))
                    verify(publicKeyManager, never()).fillGap()
                }
            }
        }
    }

})
