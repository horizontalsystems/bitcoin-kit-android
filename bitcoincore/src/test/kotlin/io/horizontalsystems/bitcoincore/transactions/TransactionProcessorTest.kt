package io.horizontalsystems.bitcoincore.transactions

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoincore.Fixtures
import io.horizontalsystems.bitcoincore.blocks.IBlockchainDataListener
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.managers.AddressManager
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.anyObject
import org.mockito.Mockito.mock

class TransactionProcessorTest {

    private val storage = mock(IStorage::class.java)
    private val outputsCache = mock(OutputsCache::class.java)
    private val extractor = mock(TransactionExtractor::class.java)
    private val addressManager = mock(AddressManager::class.java)
    private val blockchainDataListener = mock(IBlockchainDataListener::class.java)

    private val fullTransaction = Fixtures.transactionP2PKH
    private val transaction = fullTransaction.header

    private lateinit var processor: TransactionProcessor

    @Before
    fun setup() {
        processor = TransactionProcessor(storage, extractor, outputsCache, addressManager, blockchainDataListener)
    }

    @Test
    fun process() {
        processor.processOutgoing(fullTransaction)

        Mockito.verify(extractor).extractOutputs(fullTransaction)
        Mockito.verify(outputsCache).hasOutputs(fullTransaction.inputs)
        Mockito.verify(blockchainDataListener).onTransactionsUpdate(com.nhaarman.mockito_kotlin.check {
            Assert.assertArrayEquals(transaction.hash, it.firstOrNull()?.hash)
        }, eq(listOf()), anyObject())
    }

    @Test
    fun process_isMine() {
        transaction.isMine = true

        processor.processOutgoing(fullTransaction)

        Mockito.verify(extractor).extractOutputs(fullTransaction)
        Mockito.verify(extractor).extractInputs(fullTransaction)
        Mockito.verify(extractor).extractAddress(fullTransaction)
        Mockito.verify(outputsCache).add(fullTransaction.outputs)
    }

    @Test
    fun transactionsInTopologicalOrder() {
        var calledTransactions = mutableListOf<FullTransaction>()

        whenever(extractor.extractInputs(any())).then {
            calledTransactions.add(it.arguments[0] as FullTransaction)
        }

        whenever(extractor.extractOutputs(any())).then {
            calledTransactions.add(it.arguments[0] as FullTransaction)
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

                        processor.processIncoming(passedTransactions, null, false)

                        Assert.assertEquals(passedTransactions.size, calledTransactions.size)
                        // calledTransactions.forEachIndexed { index, transaction ->
                        //     assertEquals(transactions[index].header.hashHexReversed, transaction.hashHexReversed)
                        // }
                    }
                }
            }
        }
    }

    @Test
    fun testProcessTransactions_SeveralMempoolTransactions() {
        val transactions = transactions()

        for (transaction in transactions) {
            transaction.header.isMine = true
            transaction.header.timestamp = 0
            transaction.header.order = 0
        }

        transactions[1].header.status = Transaction.Status.NEW

        processor.processIncoming(listOf(transactions[3], transactions[1], transactions[2], transactions[0]), null, false)

        transactions.forEachIndexed { index, _ ->
            Assert.assertEquals(transactions[index].header.status, Transaction.Status.RELAYED)
        }
    }

    @Test
    fun testProcessTransactions_SeveralTransactionsInBlock() {
        val transactions = transactions()

        val block = Fixtures.block1

        for (transaction in transactions) {
            transaction.header.isMine = true
            transaction.header.timestamp = 0
            transaction.header.order = 0
        }

        processor.processIncoming(listOf(transactions[3], transactions[1], transactions[2], transactions[0]), block, false)

        transactions.forEachIndexed { index, _ ->
            Assert.assertEquals(transactions[index].header.status, Transaction.Status.RELAYED)
            Assert.assertEquals(transactions[index].header.timestamp, block.timestamp)
        }
    }

    private fun transactions(): List<FullTransaction> {

        val tx1 = FullTransaction(
                header = Transaction(),
                inputs = listOf(TransactionInput(
                        previousOutputTxHash = byteArrayOf(),
                        previousOutputIndex = 1
                )),
                outputs = listOf(TransactionOutput())
        )
        val tx2 = FullTransaction(
                header = Transaction(),
                inputs = listOf(TransactionInput(
                        previousOutputTxHash = tx1.header.hash,
                        previousOutputIndex = 0
                )),
                outputs = listOf(
                        TransactionOutput().apply { index = 0 },
                        TransactionOutput().apply { index = 1 })
        )
        val tx3 = FullTransaction(
                header = Transaction(),
                inputs = listOf(TransactionInput(
                        previousOutputTxHash = tx2.header.hash,
                        previousOutputIndex = 0
                )),
                outputs = listOf(TransactionOutput().apply { index = 0 })
        )
        val tx4 = FullTransaction(
                header = Transaction(),
                inputs = listOf(
                        TransactionInput(
                                previousOutputTxHash = tx2.header.hash,
                                previousOutputIndex = 0
                        ),
                        TransactionInput(
                                previousOutputTxHash = tx3.header.hash,
                                previousOutputIndex = 0
                        )),
                outputs = listOf(TransactionOutput().apply { index = 0 })
        )

        return listOf(tx1, tx2, tx3, tx4)
    }
}
