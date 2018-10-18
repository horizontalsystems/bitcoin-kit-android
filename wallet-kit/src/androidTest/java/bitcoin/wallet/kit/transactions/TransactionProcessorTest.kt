package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.RealmFactoryMock
import bitcoin.wallet.kit.managers.AddressManager
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.utils.AddressConverter
import bitcoin.wallet.kit.models.TransactionOutput
import bitcoin.wallet.kit.network.MainNet
import bitcoin.wallet.kit.network.TestNet
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class TransactionProcessorTest {

    private val factory = RealmFactoryMock()
    private val realmFactory = factory.realmFactory
    private var realm = realmFactory.realm
    private val linker = mock(TransactionLinker::class.java)
    private val extractor = mock(TransactionExtractor::class.java)
    private val addressManager = mock(AddressManager::class.java)
    private val addressConverter = mock(AddressConverter::class.java)
    private val network = MainNet()

    lateinit var processor: TransactionProcessor

    @Before
    fun setup() {
        processor = TransactionProcessor(realmFactory, addressManager, addressConverter, extractor, linker)
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
            hashHexReversed = "1"
            processed = true
        })

        val transaction2 = realm.copyToRealm(Transaction().apply {
            hashHexReversed = "2"
            processed = false
        })

        realm.commitTransaction()

        processor.enqueueRun()

        verify(extractor).extract(transaction2, realm)
        verify(extractor, never()).extract(transaction1, realm)

        verify(linker).handle(transaction2, realm)
        verify(linker, never()).handle(transaction1, realm)
        verify(addressManager).fillGap()

        Assert.assertEquals(transaction2.processed, true)
    }

    @Test
    fun run_withoutTransaction() {
        processor.enqueueRun()

        verifyZeroInteractions(extractor)
        verifyZeroInteractions(linker)
        verifyZeroInteractions(addressManager)
    }
}
