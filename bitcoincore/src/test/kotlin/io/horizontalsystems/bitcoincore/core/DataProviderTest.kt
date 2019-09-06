package io.horizontalsystems.bitcoincore.core

import com.nhaarman.mockitokotlin2.*
import io.horizontalsystems.bitcoincore.extensions.toReversedByteArray
import io.horizontalsystems.bitcoincore.managers.UnspentOutputProvider
import io.horizontalsystems.bitcoincore.models.Transaction
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object DataProviderTest : Spek({
    val storage = mock<IStorage>()
    val unspentOutputProvider = mock<UnspentOutputProvider>()
    val transactionInfoConverter = mock<ITransactionInfoConverter>()

    val dataProvider by memoized {
        DataProvider(storage, unspentOutputProvider, transactionInfoConverter)
    }

    afterEachTest {
        reset(storage)
    }

    describe("with `fromHash`") {
        val fromHash = "1234"
        val limit = 1

        it("gets transaction with given hash") {
            dataProvider.transactions(fromHash).test().assertOf {
                verify(storage).getTransaction(fromHash.toReversedByteArray())
            }
        }

        context("when transactions exist with given hash") {
            val fromTransaction = mock<Transaction>()

            beforeEach {
                whenever(storage.getTransaction(fromHash.toReversedByteArray())).thenReturn(fromTransaction)
            }

            it("starts loading transactions from that transaction") {
                dataProvider.transactions(fromHash, limit).test().assertOf {
                    verify(storage).getTransaction(fromHash.toReversedByteArray())

                    verify(storage).getFullTransactionInfo(fromTransaction, limit)
                }
            }
        }

        context("when transactions does not exist with given hash") {
            beforeEach {
                whenever(storage.getTransaction(fromHash.toReversedByteArray())).thenReturn(null)
            }

            it("do not fetch transactions with `fromHash`") {
                dataProvider.transactions(fromHash, limit).test().assertOf {
                    verify(storage).getTransaction(fromHash.toReversedByteArray())
                    verify(storage, never()).getFullTransactionInfo(null, limit)
                }
            }
        }
    }

    describe("without `fromHash`") {
        it("loads transactions without starting point") {
            dataProvider.transactions(null, null).test().assertOf {
                verify(storage, never()).getTransaction(any())

                verify(storage).getFullTransactionInfo(null, null)
            }
        }
    }
})
