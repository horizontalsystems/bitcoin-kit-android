package io.horizontalsystems.bitcoinkit.core

import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.bitcoinkit.managers.UnspentOutputProvider
import io.horizontalsystems.bitcoinkit.models.Transaction
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class DataProviderTest : Spek({
    val storage = mock<IStorage>()
    val unspentOutputProvider = mock<UnspentOutputProvider>()

    val dataProvider by memoized {
        DataProvider(storage, unspentOutputProvider)
    }

    afterEachTest {
        reset(storage)
    }

    describe("with `fromHash`") {
        val fromHash = "hash"
        val limit = 1

        it("gets transaction with given hash") {
            dataProvider.transactions(fromHash).test().assertOf {
                verify(storage).getTransaction(fromHash)
            }
        }

        context("when transactions exist with given hash") {
            val fromTransaction = mock<Transaction>()

            beforeEach {
                whenever(storage.getTransaction(fromHash)).thenReturn(fromTransaction)
            }

            it("starts loading transactions from that transaction") {
                dataProvider.transactions(fromHash, limit).test().assertOf {
                    verify(storage).getTransaction(fromHash)

                    verify(storage).getFullTransactionInfo(fromTransaction, limit)
                }
            }
        }

        context("when transactions does not exist with given hash") {
            beforeEach {
                whenever(storage.getTransaction(fromHash)).thenReturn(null)
            }

            it("loads transactions without starting point") {
                dataProvider.transactions(fromHash, limit).test().assertOf {
                    verify(storage).getTransaction(fromHash)

                    verify(storage).getFullTransactionInfo(null, limit)
                }
            }
        }
    }

    describe("with `fromHash`") {
        it("loads transactions without starting point") {
            dataProvider.transactions(null, null).test().assertOf {
                verify(storage, never()).getTransaction(any())

                verify(storage).getFullTransactionInfo(null, null)
            }
        }
    }
})
