package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.Fixtures
import bitcoin.wallet.kit.MockFactory
import bitcoin.wallet.kit.headers.HeaderHandler
import bitcoin.wallet.kit.headers.HeaderSyncer
import bitcoin.wallet.kit.models.Header
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.logging.Logger

@RunWith(PowerMockRunner::class)
@PrepareForTest(Syncer::class, Logger::class)

class SyncerTest {

    private val factory = MockFactory()
    private val headerSyncer = factory.headerSyncer
    private val headerHandler = factory.headerHandler

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

        syncer = Syncer(factory.realmFactory, factory.peerGroup, factory.network)
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
