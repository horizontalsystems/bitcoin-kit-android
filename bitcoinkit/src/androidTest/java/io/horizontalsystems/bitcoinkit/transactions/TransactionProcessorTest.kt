package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.RealmFactoryMock
import io.horizontalsystems.bitcoinkit.models.Transaction
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class TransactionProcessorTest {

    private val factory = RealmFactoryMock()
    private val realmFactory = factory.realmFactory
    private var realm = realmFactory.realm
    private val linker = mock(TransactionLinker::class.java)
    private val extractor = mock(TransactionExtractor::class.java)

    lateinit var processor: TransactionProcessor

    @Before
    fun setup() {
        processor = TransactionProcessor(extractor, linker)
    }

    @Test
    fun process() {
        val transaction = mock(Transaction::class.java)

        processor.process(transaction, realm)

        verify(extractor).extract(transaction, realm)
        verify(extractor).extract(transaction, realm)
    }

}
