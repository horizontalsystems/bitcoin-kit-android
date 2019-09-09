package io.horizontalsystems.bitcoincore.transactions

import com.nhaarman.mockitokotlin2.*
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.managers.BloomFilterManager
import io.horizontalsystems.bitcoincore.managers.PublicKeyManager
import io.horizontalsystems.bitcoincore.models.SentTransaction
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import org.junit.Assert.assertEquals
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TransactionSyncerTest : Spek({
    lateinit var syncer: TransactionSyncer

    val storage = mock(IStorage::class.java)
    val transactionProcessor = mock(TransactionProcessor::class.java)
    val publicKeyManager = mock(PublicKeyManager::class.java)

    val fullTransaction = mock(FullTransaction::class.java)
    val transaction = mock(Transaction::class.java)

    val maxRetriesCount = 3
    val retriesPeriod: Long = 60 * 1000
    val totalRetriesPeriod: Long = 60 * 60 * 24 * 1000

    beforeEachTest {
        whenever(transaction.hash).thenReturn(byteArrayOf(1, 2, 3))
        whenever(fullTransaction.header).thenReturn(transaction)

        syncer = TransactionSyncer(storage, transactionProcessor, publicKeyManager)
    }

    afterEachTest {
        reset(storage, transactionProcessor, publicKeyManager)
    }

    describe("handleTransactions") {
        context("when empty array is given") {
            it("doesn't do anything") {
                syncer.handleTransactions(listOf())

                verify(transactionProcessor, never()).processIncoming(any(), any(), any())
                verify(publicKeyManager, never()).fillGap()
            }
        }

        context("when not empty array is given") {
            val transactions = listOf(fullTransaction)

            context("when need to update bloom filter") {
                beforeEach {
                    whenever(transactionProcessor.processIncoming(eq(transactions), eq(null), eq(true)))
                            .thenThrow(BloomFilterManager.BloomFilterExpired)
                }

                it("fills addresses gap and regenerates bloom filter") {
                    syncer.handleTransactions(transactions)

                    verify(transactionProcessor).processIncoming(eq(transactions), eq(null), eq(true))
                    verify(publicKeyManager).fillGap()
                }
            }

            context("when don't need to update bloom filter") {
                it("doesn't run address fillGap and doesn't regenerate bloom filter") {
                    syncer.handleTransactions(transactions)

                    verify(transactionProcessor).processIncoming(eq(transactions), eq(null), eq(true))
                    verify(publicKeyManager, never()).fillGap()
                }
            }
        }
    }

    describe("#handleTransaction (sent transaction)") {

        context("when SentTransaction does not exist") {
            beforeEach {
                whenever(storage.getNewTransaction(transaction.hash)).thenReturn(transaction)
                whenever(storage.getSentTransaction(transaction.hash)).thenReturn(null)
            }

            it("adds new SentTransaction object") {
                argumentCaptor<SentTransaction>().apply {
                    syncer.handleTransaction(fullTransaction)

                    verify(storage).addSentTransaction(capture())
                    firstValue.hash = transaction.hash
                }
            }
        }

        context("when SentTransaction exists") {
            val sentTransaction = SentTransaction()

            beforeEach {
                sentTransaction.apply {
                    hash = transaction.hash
                    firstSendTime = sentTransaction.firstSendTime - 100
                    lastSendTime = sentTransaction.lastSendTime - 100
                }

                whenever(storage.getNewTransaction(transaction.hash)).thenReturn(transaction)
                whenever(storage.getSentTransaction(transaction.hash)).thenReturn(sentTransaction)
            }

            it("updates existing SentTransaction object") {
                argumentCaptor<SentTransaction>().apply {
                    syncer.handleTransaction(fullTransaction)

                    verify(storage).updateSentTransaction(capture())
                    firstValue.hash = transaction.hash
                }
            }
        }

        context("when Transaction doesn't exist") {
            beforeEach {
                whenever(storage.getNewTransaction(transaction.hash)).thenReturn(null)
            }

            it("neither adds new nor updates existing") {
                syncer.handleTransaction(fullTransaction)

                verify(storage, never()).addSentTransaction(any())
                verify(storage, never()).updateSentTransaction(any())
            }
        }
    }

    describe("#getPendingTransactions") {

        context("when transaction is :NEW") {
            beforeEach {
                whenever(storage.getNewTransactions()).thenReturn(listOf(fullTransaction))
            }

            context("when it wasn't sent") {
                beforeEach {
                    whenever(storage.getSentTransaction(transaction.hash)).thenReturn(null)
                }

                it("returns transaction") {
                    val transactions = syncer.getPendingTransactions()

                    assertEquals(1, transactions.size)
                    assertEquals(transaction, transactions[0].header)
                }

            }

            context("when it was sent") {
                val sentTransaction = SentTransaction()

                beforeEach {
                    sentTransaction.apply {
                        hash = transaction.hash
                        lastSendTime = System.currentTimeMillis() - retriesPeriod - 1
                        retriesCount = 0
                    }

                    whenever(storage.getSentTransaction(transaction.hash)).thenReturn(sentTransaction)
                }

                context("when sent not too many times or too frequently") {
                    it("returns transaction") {
                        val transactions = syncer.getPendingTransactions()

                        assertEquals(1, transactions.size)
                        assertEquals(transaction.hash, transactions[0].header.hash)
                    }
                }

                context("when sent too many times") {
                    it("doesn't return any transaction") {
                        sentTransaction.retriesCount = maxRetriesCount

                        assertEquals(0, syncer.getPendingTransactions().size)
                    }
                }

                context("when sent too often") {
                    it("doesn't return any transaction") {
                        sentTransaction.lastSendTime = System.currentTimeMillis()

                        assertEquals(0, syncer.getPendingTransactions().size)
                    }
                }

                context("when sent too often in totalRetriesPeriod period") {
                    it("doesn't return any transaction") {
                        sentTransaction.firstSendTime = System.currentTimeMillis() - totalRetriesPeriod - 1

                        assertEquals(0, syncer.getPendingTransactions().size)
                    }
                }
            }
        }

        context("when transaction is not :NEW") {
            beforeEach {
                whenever(storage.getNewTransactions()).thenReturn(listOf())
            }

            it("doesn't return transaction") {
                assertEquals(0, syncer.getPendingTransactions().size)
            }
        }
    }
})
