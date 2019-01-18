package io.horizontalsystems.bitcoinkit.transactions

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import helpers.Fixtures
import io.horizontalsystems.bitcoinkit.RealmFactoryMock
import io.horizontalsystems.bitcoinkit.managers.AddressManager
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.models.TransactionInput
import io.horizontalsystems.bitcoinkit.models.TransactionOutput
import io.realm.Realm
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class TransactionProcessorTest {

    private val factory = RealmFactoryMock()
    private lateinit var realm: Realm
    private val transaction = mock(Transaction::class.java)
    private val linker = mock(TransactionLinker::class.java)
    private val extractor = mock(TransactionExtractor::class.java)
    private val addressManager = mock(AddressManager::class.java)

    private lateinit var processor: TransactionProcessor

    @Before
    fun setup() {
        realm = factory.realmFactory.realm
        realm.executeTransaction {
            it.deleteAll()
        }
        processor = TransactionProcessor(extractor, linker, addressManager)
    }

    @Test
    fun process() {
        whenever(transaction.isMine).thenReturn(false)
        processor.process(transaction, realm)

        verify(extractor).extractOutputs(transaction, realm)
        verify(linker).handle(transaction, realm)
    }

    @Test
    fun process_isMine() {
        whenever(transaction.isMine).thenReturn(true)
        processor.process(transaction, realm)

        verify(extractor).extractOutputs(transaction, realm)
        verify(extractor).extractInputs(transaction)
        verify(extractor).extractAddress(transaction)
        verify(linker).handle(transaction, realm)
    }

    @Test
    fun transactionsInTopologicalOrder() {
        var calledTransactions = mutableListOf<Transaction>()

        whenever(extractor.extractInputs(any())).then {
            calledTransactions.add(it.arguments[0] as Transaction)
        }

        whenever(extractor.extractOutputs(any(), any())).then {
            calledTransactions.add(it.arguments[0] as Transaction)
        }

        val transactions = transactions()

        for (i in 0 until 4) {
            for (j in 0 until 4) {
                for (k in 0 until 4) {
                    for (l in 0 until 4) {
                        if (listOf(0, 1, 2, 3).intersect(listOf(i, j, k, l)).size < 4)
                            continue

                        calledTransactions = mutableListOf()

                        val passedTransactions = listOf(transactions[i], transactions[j], transactions[k], transactions[l])

                        processor.process(passedTransactions, null, false, realm)

                        assertEquals(passedTransactions.size, calledTransactions.size)
                        calledTransactions.forEachIndexed { index, transaction ->
                            assertEquals(transactions[index].hashHexReversed, transaction.hashHexReversed)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testProcessTransactions_SeveralMempoolTransactions() {
        val transactions = transactions()

        for (transaction in transactions) {
            transaction.isMine = true
            transaction.timestamp = 0
            transaction.order = 0
        }

        realm.executeTransaction {
            transactions[1].status = Transaction.Status.NEW
            it.insert(transactions[1])
            it.insert(transactions[3])

            processor.process(listOf(transactions[3], transactions[1], transactions[2], transactions[0]), null, false, it)
        }

        val realmTransactions = realm.where(Transaction::class.java).sort("order").findAll()

        assertEquals(4, realmTransactions.count())

        transactions.forEachIndexed { index, transaction ->
            assertArrayEquals(realmTransactions[index]?.hash, transaction.hash)
            assertEquals(realmTransactions[index]?.status, Transaction.Status.RELAYED)
            assertEquals(realmTransactions[index]?.order, index)
        }
    }

    @Test
    fun testProcessTransactions_SeveralTransactionsInBlock() {
        val transactions = transactions()

        val block = Fixtures.block1

        for (transaction in transactions) {
            transaction.isMine = true
            transaction.timestamp = 0
            transaction.order = 0
        }

        realm.executeTransaction {
            transactions[1].status = Transaction.Status.NEW
            it.insert(transactions[1])
            it.insert(transactions[3])
            val managedBlock = it.copyToRealm(block)

            processor.process(listOf(transactions[3], transactions[1], transactions[2], transactions[0]), managedBlock, false, it)
        }

        val realmTransactions = realm.where(Transaction::class.java).sort("order").findAll()

        assertEquals(4, realmTransactions.count())

        transactions.forEachIndexed { index, transaction ->
            assertArrayEquals(realmTransactions[index]?.hash, transaction.hash)
            assertEquals(realmTransactions[index]?.block?.reversedHeaderHashHex, block.reversedHeaderHashHex)
            assertEquals(realmTransactions[index]?.status, Transaction.Status.RELAYED)
            assertEquals(realmTransactions[index]?.order, index)
            assertEquals(realmTransactions[index]?.timestamp, block.header?.timestamp)
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
