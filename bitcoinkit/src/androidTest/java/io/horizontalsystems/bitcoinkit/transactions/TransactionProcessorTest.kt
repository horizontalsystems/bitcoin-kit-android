package io.horizontalsystems.bitcoinkit.transactions

import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.RealmFactoryMock
import io.horizontalsystems.bitcoinkit.managers.AddressManager
import io.horizontalsystems.bitcoinkit.models.Transaction
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class TransactionProcessorTest {

    private val factory = RealmFactoryMock()
    private val realmFactory = factory.realmFactory
    private var realm = realmFactory.realm
    private val transaction = mock(Transaction::class.java)
    private val linker = mock(TransactionLinker::class.java)
    private val extractor = mock(TransactionExtractor::class.java)
    private val addressManager = mock(AddressManager::class.java)

    private lateinit var processor: TransactionProcessor

    @Before
    fun setup() {
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
}
