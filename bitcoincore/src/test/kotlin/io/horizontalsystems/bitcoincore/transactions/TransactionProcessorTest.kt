package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.Fixtures
import io.horizontalsystems.bitcoincore.blocks.IBlockchainDataListener
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.managers.BloomFilterManager
import io.horizontalsystems.bitcoincore.managers.IIrregularOutputFinder
import io.horizontalsystems.bitcoincore.managers.PublicKeyManager
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import org.junit.Assert
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TransactionProcessorTest : Spek({

    lateinit var processor: PendingTransactionProcessor
    lateinit var transaction: Transaction
    lateinit var fullTransaction: FullTransaction

    val storage = mock(IStorage::class.java)
    val outputsCache = mock(OutputsCache::class.java)
    val extractor = mock(TransactionExtractor::class.java)
    val publicKeyManager = mock(PublicKeyManager::class.java)
    val blockchainDataListener = mock(IBlockchainDataListener::class.java)
    val irregularOutputFinder = mock(IIrregularOutputFinder::class.java)
    val conflictsResolver = mock(TransactionConflictsResolver::class.java)

    beforeEachTest {
        fullTransaction = Fixtures.transactionP2PKH
        transaction = fullTransaction.header
        processor = PendingTransactionProcessor(storage, extractor, publicKeyManager, irregularOutputFinder, blockchainDataListener, conflictsResolver)
    }

    fun transactions(): List<FullTransaction> {

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

    describe("process") {

        it("process") {
            processor.processCreated(fullTransaction)

            Mockito.verify(extractor).extractOutputs(fullTransaction)
            Mockito.verify(outputsCache).hasOutputs(fullTransaction.inputs)
            // Mockito.verify(blockchainDataListener).onTransactionsUpdate(check {
            //     Assert.assertArrayEquals(transaction.hash, it.firstOrNull()?.hash)
            // }, eq(listOf()), any())
        }

        it("process_isMine") {
            transaction.isMine = true

            processor.processCreated(fullTransaction)

            Mockito.verify(extractor).extractOutputs(fullTransaction)
            Mockito.verify(extractor).extractInputs(fullTransaction)
            Mockito.verify(extractor).extractAddress(fullTransaction)
            Mockito.verify(outputsCache).add(fullTransaction.outputs)
        }

        it("testProcessTransactions_SeveralMempoolTransactions") {
            val transactions = transactions()

            for (transaction in transactions) {
                transaction.header.isMine = true
                transaction.header.timestamp = 0
                transaction.header.order = 0
            }

            transactions[1].header.status = Transaction.Status.NEW

            try {
                processor.processReceived(listOf(transactions[3], transactions[1], transactions[2], transactions[0]), false)

                transactions.forEachIndexed { index, _ ->
                    Assert.assertEquals(transactions[index].header.status, Transaction.Status.RELAYED)
                }
            } catch (e: BloomFilterManager.BloomFilterExpired) {
            }
        }

        it("testProcessTransactions_SeveralTransactionsInBlock") {
            val transactions = transactions()

            val block = Fixtures.block1

            for (transaction in transactions) {
                transaction.header.isMine = true
                transaction.header.timestamp = 0
                transaction.header.order = 0
            }

            processor.processReceived(listOf(transactions[3], transactions[1], transactions[2], transactions[0]), false)

            transactions.forEachIndexed { index, _ ->
                Assert.assertEquals(transactions[index].header.status, Transaction.Status.RELAYED)
                Assert.assertEquals(transactions[index].header.timestamp, block.timestamp)
            }
        }
    }

})
