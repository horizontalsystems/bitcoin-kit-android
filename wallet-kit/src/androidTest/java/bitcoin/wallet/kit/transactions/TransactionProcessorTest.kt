package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.Factories
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.network.TestNet
import com.nhaarman.mockito_kotlin.any
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class TransactionProcessorTest {

    private val factory = Factories()
    private val realmFactory = factory.realmFactory
    private var realm = realmFactory.realm
    private val linker = factory.transactionLinker
    private val extractor = mock(TransactionExtractor::class.java)

    lateinit var processor: TransactionProcessor

    @Before
    fun setup() {
        processor = TransactionProcessor(realmFactory, TestNet(), extractor, linker)
    }

    @After
    fun teardown() {
        realm.executeTransaction {
            it.deleteAll()
        }
    }

    @Test
    fun run() {
        realm.beginTransaction()

        val transaction1 = realm.copyToRealm(Transaction().apply {
            reversedHashHex = "1"
            processed = true
        })

        val transaction2 = realm.copyToRealm(Transaction().apply {
            reversedHashHex = "2"
            processed = false
        })

        realm.commitTransaction()

        processor.enqueueRun()
        verify(extractor).extract(transaction2)
        verify(extractor, never()).extract(transaction1)

        verify(linker).handle(transaction2)
        verify(linker, never()).handle(transaction1)

        Assert.assertEquals(transaction2.processed, true)
    }

    @Test
    fun run_withoutTransaction() {
        processor.enqueueRun()
        verify(extractor, never()).extract(any())
        verify(linker, never()).handle(any())
    }
}
