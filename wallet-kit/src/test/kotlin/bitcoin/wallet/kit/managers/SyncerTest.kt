package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.headers.HeaderHandler
import bitcoin.wallet.kit.headers.HeaderSyncer
import bitcoin.wallet.kit.models.Header
import bitcoin.wallet.kit.network.PeerGroup
import bitcoin.wallet.kit.network.TestNet
import bitcoin.wallet.kit.transactions.TransactionProcessor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import helpers.Fixtures
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.logging.Logger

@RunWith(PowerMockRunner::class)
@PrepareForTest(Syncer::class, Logger::class)

class SyncerTest {

    private val transactionProcessor = mock(TransactionProcessor::class.java)
    private val headerSyncer = mock(HeaderSyncer::class.java)
    private val headerHandler = mock(HeaderHandler::class.java)
    private val realmFactory = mock(RealmFactory::class.java)
    private val peerGroup = mock(PeerGroup::class.java)
    private val network = mock(TestNet::class.java)

    lateinit var syncer: Syncer

    @Before
    fun setup() {
        // HeaderSyncer
        PowerMockito
                .whenNew(HeaderSyncer::class.java)
                .withAnyArguments()
                .thenReturn(headerSyncer)

        // HeaderHandler
        PowerMockito
                .whenNew(HeaderHandler::class.java)
                .withAnyArguments()
                .thenReturn(headerHandler)

        syncer = Syncer(realmFactory, peerGroup, transactionProcessor, network)
    }

    @Test
    fun onReady_headerSync() {
        syncer.onReady()
        verify(headerSyncer).sync()
    }

    @Test(expected = Exception::class)
    fun onReady_headerSync_error() {
        whenever(headerSyncer.sync())
                .thenThrow(Exception())

        syncer.onReady()
    }

    @Test
    fun onReceiveHeaders() {
        val headers = arrayOf(Fixtures.block1.header!!, Fixtures.block2.header!!)

        syncer.onReceiveHeaders(headers)
        verify(headerHandler).handle(headers)
    }

    @Test(expected = Exception::class)
    fun onReceiveHeaders_error() {
        val headers = arrayOf<Header>()
        whenever(headerHandler.handle(headers))
                .thenThrow(Exception())

        syncer.onReceiveHeaders(headers)
        verify(headerHandler).handle(headers)
    }

}
