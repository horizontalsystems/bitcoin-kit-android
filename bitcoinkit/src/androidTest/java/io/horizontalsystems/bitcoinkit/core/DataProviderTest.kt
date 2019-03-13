package io.horizontalsystems.bitcoinkit.core

import io.horizontalsystems.bitcoinkit.RealmFactoryMock
import io.horizontalsystems.bitcoinkit.managers.UnspentOutputProvider
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.models.TransactionInput
import io.horizontalsystems.bitcoinkit.models.TransactionOutput
import io.realm.Realm
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock

class DataProviderTest {

    private val storage = mock(IStorage::class.java)
    private val factory = RealmFactoryMock()
    private val listener = Mockito.mock(DataProvider.Listener::class.java)
    private val unspentOutputProvider = Mockito.mock(UnspentOutputProvider::class.java)


    private lateinit var dataProvider: DataProvider
    private lateinit var realm: Realm

    @Before
    fun setUp() {
        realm = factory.realmFactory.realm
        realm.executeTransaction {
            it.deleteAll()
        }
        dataProvider = DataProvider(storage, factory.realmFactory, listener, unspentOutputProvider)
    }

    @Test
    fun testTransactions() {
        val transactions = transactions()
        transactions[0].timestamp = 1000005
        transactions[3].timestamp = 1000002
        transactions[1].timestamp = 1000001
        transactions[2].timestamp = 1000000

        realm.executeTransaction {
            it.insert(transactions)
        }

        dataProvider.transactions(limit = 3).test().assertValue {
            it.size == 3 &&
                    it[0].transactionHash == transactions[0].hashHexReversed &&
                    it[1].transactionHash == transactions[3].hashHexReversed &&
                    it[2].transactionHash == transactions[1].hashHexReversed
        }
    }

    @Test
    fun testTransactions_WithEqualTimestamps() {
        val transactions = transactions()
        transactions[2].apply {
            timestamp = 1000005
            order = 1
        }
        transactions[0].apply {
            timestamp = 1000005
            order = 0
        }
        transactions[3].apply {
            timestamp = 1000001
            order = 1
        }
        transactions[1].apply {
            timestamp = 1000001
            order = 0
        }

        realm.executeTransaction {
            it.insert(transactions)
        }

        dataProvider.transactions(limit = 3).test().assertValue {
            it.size == 3 &&
                    it[0].transactionHash == transactions[2].hashHexReversed &&
                    it[1].transactionHash == transactions[0].hashHexReversed &&
                    it[2].transactionHash == transactions[3].hashHexReversed
        }
    }

    @Test
    fun testTransactions_FromHashGiven() {
        val transactions = transactions()
        transactions[2].apply {
            timestamp = 1000005
            order = 1
        }
        transactions[0].apply {
            timestamp = 1000005
            order = 0
        }
        transactions[3].apply {
            timestamp = 1000001
            order = 1
        }
        transactions[1].apply {
            timestamp = 1000001
            order = 0
        }

        realm.executeTransaction {
            it.insert(transactions)
        }

        dataProvider.transactions(fromHash = transactions[3].hashHexReversed, limit = 3).test().assertValue {
            it.size == 1 &&
                    it[0].transactionHash == transactions[1].hashHexReversed
        }
    }

    @Test
    fun testTransactions_LimitNotGiven() {
        val transactions = transactions()
        transactions[2].apply {
            timestamp = 1000005
            order = 1
        }
        transactions[0].apply {
            timestamp = 1000005
            order = 0
        }
        transactions[3].apply {
            timestamp = 1000001
            order = 1
        }
        transactions[1].apply {
            timestamp = 1000001
            order = 0
        }

        realm.executeTransaction {
            it.insert(transactions)
        }

        dataProvider.transactions().test().assertValue {
            it.size == 4 &&
                    it[0].transactionHash == transactions[2].hashHexReversed &&
                    it[1].transactionHash == transactions[0].hashHexReversed &&
                    it[2].transactionHash == transactions[3].hashHexReversed &&
                    it[3].transactionHash == transactions[1].hashHexReversed
        }
    }

    private fun transactions(): List<Transaction> {

        val tx1 = Transaction().apply {
            inputs.add(TransactionInput().apply {
                previousOutputHash = byteArrayOf(1)
                previousOutputIndex = 1
            })
            outputs.add(TransactionOutput())
            setHashes()
        }

        val tx2 = Transaction().apply {
            inputs.add(TransactionInput().apply {
                previousOutputHexReversed = tx1.hashHexReversed
                previousOutputIndex = 0
            })
            outputs.add(TransactionOutput().apply {
                index = 0
            })
            outputs.add(TransactionOutput().apply {
                index = 1
            })
            setHashes()
        }

        val tx3 = Transaction().apply {
            inputs.add(TransactionInput().apply {
                previousOutputHexReversed = tx2.hashHexReversed
                previousOutputIndex = 0
            })
            outputs.add(TransactionOutput().apply {
                index = 0
            })
            setHashes()
        }

        val tx4 = Transaction().apply {
            inputs.add(TransactionInput().apply {
                previousOutputHexReversed = tx2.hashHexReversed
                previousOutputIndex = 0
            })
            inputs.add(TransactionInput().apply {
                previousOutputHexReversed = tx3.hashHexReversed
                previousOutputIndex = 0
            })
            outputs.add(TransactionOutput().apply {
                index = 0
            })
            setHashes()
        }

        return listOf(tx1, tx2, tx3, tx4)
    }

}
