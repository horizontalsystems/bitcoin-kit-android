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
        val fromTimestamp = 1234567L
        val limit = 1

        it("gets transaction with given hash") {
            dataProvider.transactions(fromHash, fromTimestamp).test().assertOf {
                verify(storage).getValidOrInvalidTransaction(fromHash.toReversedByteArray(), fromTimestamp)
            }
        }

        context("when transactions exist with given hash and timestamp") {
            val fromTransaction = mock<Transaction>()

            beforeEach {
                whenever(storage.getValidOrInvalidTransaction(fromHash.toReversedByteArray(), fromTimestamp)).thenReturn(fromTransaction)
            }

            it("starts loading transactions from that transaction") {
                dataProvider.transactions(fromHash, fromTimestamp, limit).test().assertOf {
                    verify(storage).getValidOrInvalidTransaction(fromHash.toReversedByteArray(), fromTimestamp)

                    verify(storage).getFullTransactionInfo(fromTransaction, limit)
                }
            }
        }

        context("when transactions does not exist with given hash and timestamp") {
            beforeEach {
                whenever(storage.getValidOrInvalidTransaction(fromHash.toReversedByteArray(), fromTimestamp)).thenReturn(null)
            }

            it("do not fetch transactions with `fromHash` and `fromTimestamp`") {
                dataProvider.transactions(fromHash, fromTimestamp, limit).test().assertOf {
                    verify(storage).getValidOrInvalidTransaction(fromHash.toReversedByteArray(), fromTimestamp)
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
