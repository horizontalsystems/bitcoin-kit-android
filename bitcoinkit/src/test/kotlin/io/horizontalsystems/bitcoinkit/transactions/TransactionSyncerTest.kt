package io.horizontalsystems.bitcoinkit.transactions

import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.managers.AddressManager
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.horizontalsystems.bitcoinkit.models.SentTransaction
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.realm.Realm
import org.junit.Assert.assertEquals
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.invocation.InvocationOnMock
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class TransactionSyncerTest : Spek({
    lateinit var syncer: TransactionSyncer

    val storage = mock(IStorage::class.java)
    val transactionProcessor = mock(TransactionProcessor::class.java)
    val addressManager = mock(AddressManager::class.java)
    val bloomFilterManager = mock(BloomFilterManager::class.java)

    val realm = mock(Realm::class.java)
    val transaction = mock(Transaction::class.java)
    val answerRealmInstance: (InvocationOnMock) -> Unit = {
        (it.arguments[0] as (Realm) -> Unit).invoke(realm)
    }

    val maxRetriesCount = 3
    val retriesPeriod: Long = 60 * 1000
    val totalRetriesPeriod: Long = 60 * 60 * 24 * 1000

    beforeEachTest {
        whenever(storage.inTransaction(any())).then(answerRealmInstance)
        whenever(transaction.hashHexReversed).thenReturn("abc")

        syncer = TransactionSyncer(storage, transactionProcessor, addressManager, bloomFilterManager)
    }

    afterEachTest {
        reset(storage, transactionProcessor, addressManager, bloomFilterManager)
    }

    describe("handleTransactions") {
        context("when empty array is given") {
            it("doesn't do anything") {
                syncer.handleTransactions(listOf())

                verify(transactionProcessor, never()).process(any(), any(), any(), any())
                verify(addressManager, never()).fillGap()
                verify(bloomFilterManager, never()).regenerateBloomFilter()
            }
        }

        context("when not empty array is given") {
            val transactions = listOf(transaction)

            context("when need to update bloom filter") {
                beforeEach {
                    whenever(transactionProcessor.process(eq(transactions), eq(null), eq(true), eq(realm)))
                            .thenThrow(BloomFilterManager.BloomFilterExpired)
                }

                it("fills addresses gap and regenerates bloom filter") {
                    syncer.handleTransactions(transactions)

                    verify(transactionProcessor).process(eq(transactions), eq(null), eq(true), eq(realm))
                    verify(addressManager).fillGap()
                    verify(bloomFilterManager).regenerateBloomFilter()
                }
            }

            context("when don't need to update bloom filter") {
                it("doesn't run address fillGap and doesn't regenerate bloom filter") {
                    syncer.handleTransactions(transactions)

                    verify(transactionProcessor).process(eq(transactions), eq(null), eq(true), eq(realm))
                    verify(addressManager, never()).fillGap()
                    verify(bloomFilterManager, never()).regenerateBloomFilter()
                }
            }
        }
    }

    describe("#handleTransaction (sent transaction)") {

        context("when SentTransaction does not exist") {
            beforeEach {
                whenever(storage.getNewTransaction(transaction.hashHexReversed)).thenReturn(transaction)
                whenever(storage.getSentTransaction(transaction.hashHexReversed)).thenReturn(null)
            }

            it("adds new SentTransaction object") {
                argumentCaptor<SentTransaction>().apply {
                    syncer.handleTransaction(transaction)

                    verify(storage).addSentTransaction(capture())
                    firstValue.hashHexReversed = transaction.hashHexReversed
                }
            }
        }

        context("when SentTransaction exists") {
            val sentTransaction = SentTransaction()

            beforeEach {
                sentTransaction.apply {
                    hashHexReversed = transaction.hashHexReversed
                    firstSendTime = sentTransaction.firstSendTime - 100
                    lastSendTime = sentTransaction.lastSendTime - 100
                }

                whenever(storage.getNewTransaction(transaction.hashHexReversed)).thenReturn(transaction)
                whenever(storage.getSentTransaction(transaction.hashHexReversed)).thenReturn(sentTransaction)
            }

            it("updates existing SentTransaction object") {
                argumentCaptor<SentTransaction>().apply {
                    syncer.handleTransaction(transaction)

                    verify(storage).updateSentTransaction(capture())
                    firstValue.hashHexReversed = transaction.hashHexReversed
                }
            }
        }

        context("when Transaction doesn't exist") {
            beforeEach {
                whenever(storage.getNewTransaction(transaction.hashHexReversed)).thenReturn(null)
            }

            it("neither adds new nor updates existing") {
                syncer.handleTransaction(transaction)

                verify(storage, never()).addSentTransaction(any())
                verify(storage, never()).updateSentTransaction(any())
            }
        }
    }

    describe("#getPendingTransactions") {

        context("when transaction is :NEW") {
            beforeEach {
                whenever(storage.getNewTransactions()).thenReturn(listOf(transaction))
            }

            context("when it wasn't sent") {
                beforeEach {
                    whenever(storage.getSentTransaction(transaction.hashHexReversed)).thenReturn(null)
                }

                it("returns transaction") {
                    val transactions = syncer.getPendingTransactions()

                    assertEquals(1, transactions.size)
                    assertEquals(transaction, transactions[0])
                }

            }

            context("when it was sent") {
                val sentTransaction = SentTransaction()

                beforeEach {
                    sentTransaction.apply {
                        hashHexReversed = transaction.hashHexReversed
                        lastSendTime = System.currentTimeMillis() - retriesPeriod - 1
                        retriesCount = 0
                    }

                    whenever(storage.getSentTransaction(transaction.hashHexReversed)).thenReturn(sentTransaction)
                }

                context("when sent not too many times or too frequently") {
                    it("returns transaction") {
                        val transactions = syncer.getPendingTransactions()

                        assertEquals(1, transactions.size)
                        assertEquals(transaction.hash, transactions[0].hash)
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
