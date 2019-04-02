package io.horizontalsystems.bitcoinkit.core

import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.Fixtures
import io.horizontalsystems.bitcoinkit.managers.UnspentOutputProvider
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.models.TransactionInput
import io.horizontalsystems.bitcoinkit.models.TransactionOutput
import io.horizontalsystems.bitcoinkit.storage.FullTransaction
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class DataProviderTest {

    private val storage = mock(IStorage::class.java)
    private val unspentOutputProvider = mock(UnspentOutputProvider::class.java)

    private lateinit var dataProvider: DataProvider

    @Before
    fun setUp() {
        whenever(storage.lastBlock()).thenReturn(Fixtures.checkpointBlock1)
        whenever(unspentOutputProvider.getBalance()).thenReturn(0)

        dataProvider = DataProvider(storage, unspentOutputProvider)
    }

    @Test
    fun testTransactions() {
        val transactions = transactions()

        transactions[0].header.timestamp = 1000005
        transactions[3].header.timestamp = 1000002
        transactions[1].header.timestamp = 1000001
        transactions[2].header.timestamp = 1000000

        val sorted = transactions.map { it.header }
                .sortedByDescending { it.timestamp }
                .sortedByDescending { it.order }

        whenever(storage.getTransactionsSortedTimestampAndOrdered()).thenReturn(sorted)

        dataProvider.transactions(limit = 3).test().assertValue {
            it.size == 3 &&
                    it[0].transactionHash == transactions[0].header.hashHexReversed &&
                    it[1].transactionHash == transactions[3].header.hashHexReversed &&
                    it[2].transactionHash == transactions[1].header.hashHexReversed
        }
    }

    @Test
    fun testTransactions_WithEqualTimestamps() {
        val transactions = transactions()

        transactions[2].apply {
            header.timestamp = 1000005
            header.order = 1
        }
        transactions[0].apply {
            header.timestamp = 1000005
            header.order = 0
        }
        transactions[3].apply {
            header.timestamp = 1000001
            header.order = 1
        }
        transactions[1].apply {
            header.timestamp = 1000001
            header.order = 0
        }

        val sorted = transactions.map { it.header }
                .sortedByDescending { it.timestamp }

        whenever(storage.getTransactionsSortedTimestampAndOrdered()).thenReturn(sorted)

        dataProvider.transactions(limit = 3).test().assertValue {
            it.size == 3 &&
                    it[0].timestamp == transactions[2].header.timestamp &&
                    it[1].timestamp == transactions[0].header.timestamp &&
                    it[2].timestamp == transactions[3].header.timestamp
        }
    }

    @Test
    fun testTransactions_FromHashGiven() {
        val transactions = transactions()

        transactions[2].apply {
            header.timestamp = 1000005
            header.order = 1
        }
        transactions[0].apply {
            header.timestamp = 1000005
            header.order = 0
        }
        transactions[3].apply {
            header.timestamp = 1000001
            header.order = 1
        }
        transactions[1].apply {
            header.timestamp = 1000001
            header.order = 0
        }

        val fromTransaction = transactions[3].header
        val fromHash = fromTransaction.hashHexReversed

        val sorted = transactions.map { it.header }
                .sortedByDescending { it.timestamp }
                .sortedByDescending { it.order }

        whenever(storage.getTransactionsSortedTimestampAndOrdered()).thenReturn(sorted)
        whenever(storage.getTransaction(fromHash)).thenReturn(fromTransaction)

        dataProvider.transactions(fromHash, limit = 3).test().assertValue {
            it.size == 1 && it[0].transactionHash == transactions[1].header.hashHexReversed
        }
    }

    @Test
    fun testTransactions_LimitNotGiven() {
        val transactions = transactions()

        transactions[2].apply {
            header.timestamp = 1000005
            header.order = 1
        }
        transactions[0].apply {
            header.timestamp = 1000005
            header.order = 0
        }
        transactions[3].apply {
            header.timestamp = 1000001
            header.order = 1
        }
        transactions[1].apply {
            header.timestamp = 1000001
            header.order = 0
        }

        val sorted = transactions
                .map { it.header }
                .sortedByDescending { it.timestamp }

        whenever(storage.getTransactionsSortedTimestampAndOrdered()).thenReturn(sorted)

        dataProvider.transactions().test().assertValue {

            it.size == 4 &&
                    it[0].timestamp == transactions[2].header.timestamp &&
                    it[1].timestamp == transactions[0].header.timestamp &&
                    it[2].timestamp == transactions[3].header.timestamp &&
                    it[3].timestamp == transactions[1].header.timestamp
        }
    }

    private fun transactions(): List<FullTransaction> {

        val tx1 = FullTransaction(
                header = Transaction().apply {
                    hashHexReversed = "1"
                },
                inputs = listOf(TransactionInput(
                        previousOutputTxReversedHex = "1",
                        previousOutputIndex = 1
                )),
                outputs = listOf(TransactionOutput())
        )

        val tx2 = FullTransaction(
                header = Transaction().apply {
                    hashHexReversed = "2"
                },
                inputs = listOf(TransactionInput(
                        previousOutputTxReversedHex = tx1.header.hashHexReversed,
                        previousOutputIndex = 0
                )),
                outputs = listOf(
                        TransactionOutput().apply { index = 0 },
                        TransactionOutput().apply { index = 1 })
        )

        val tx3 = FullTransaction(
                header = Transaction().apply {
                    hashHexReversed = "3"
                },
                inputs = listOf(TransactionInput(
                        previousOutputTxReversedHex = tx2.header.hashHexReversed,
                        previousOutputIndex = 0
                )),
                outputs = listOf(TransactionOutput().apply { index = 0 })
        )

        val tx4 = FullTransaction(
                header = Transaction().apply {
                    hashHexReversed = "4"
                },
                inputs = listOf(
                        TransactionInput(
                                previousOutputTxReversedHex = tx2.header.hashHexReversed,
                                previousOutputIndex = 0
                        ),
                        TransactionInput(
                                previousOutputTxReversedHex = tx3.header.hashHexReversed,
                                previousOutputIndex = 0
                        )),
                outputs = listOf(TransactionOutput().apply { index = 0 })
        )


        return listOf(tx1, tx2, tx3, tx4)
    }

}
