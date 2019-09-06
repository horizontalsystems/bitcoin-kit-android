package io.horizontalsystems.bitcoincore.network

import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.network.messages.*
import io.horizontalsystems.bitcoincore.network.peer.Peer
import io.horizontalsystems.bitcoincore.network.peer.PeerConnection
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.concurrent.ExecutorService

@RunWith(PowerMockRunner::class)
@PrepareForTest(Peer::class)

class PeerTest {

    private val listener = mock(Peer.Listener::class.java)
    private val peerConnection = mock(PeerConnection::class.java)
    private val network = mock(Network::class.java)
    private val versionMessage = mock(VersionMessage::class.java)
    private val addressMessage = mock(AddrMessage::class.java)
    private val networkMessageParser = mock(NetworkMessageParser::class.java)
    private val networkMessageSerializer = mock(NetworkMessageSerializer::class.java)
    private val executorService = mock(ExecutorService::class.java)

    private lateinit var peer: Peer

    @Before
    fun setup() {
        PowerMockito
                .whenNew(PeerConnection::class.java)
                .withAnyArguments()
                .thenReturn(peerConnection)

        peer = Peer("host", network, listener, networkMessageParser, networkMessageSerializer, executorService)
    }

    @Test
    fun onMessage_versionMessage_success() {
        whenever(versionMessage.lastBlock).thenReturn(99)
        whenever(versionMessage.hasBlockChain(network)).thenReturn(true)
        whenever(versionMessage.supportsBloomFilter(network)).thenReturn(true)

        peer.onMessage(versionMessage)

        argumentCaptor<IMessage>().apply {
            verify(peerConnection).sendMessage(capture())

            Assert.assertTrue(firstValue is VerAckMessage)
        }
    }

    @Test
    fun onMessage_versionMessage_error_lastBlockIs0() {
        whenever(versionMessage.lastBlock).thenReturn(0)

        peer.onMessage(versionMessage)

        verify(peerConnection).close(argThat {
            this is Peer.Error.UnsuitablePeerVersion
        })
    }

    @Test
    fun onMessage_versionMessage_error_notFullNode() {
        whenever(versionMessage.lastBlock).thenReturn(99)
        whenever(versionMessage.hasBlockChain(network)).thenReturn(false)

        peer.onMessage(versionMessage)

        verify(peerConnection).close(argThat {
            this is Peer.Error.UnsuitablePeerVersion
        })
    }

    @Test
    fun onMessage_versionMessage_error_notSupportingBloomFilter() {
        whenever(versionMessage.lastBlock).thenReturn(99)
        whenever(versionMessage.supportsBloomFilter(network)).thenReturn(false)

        peer.onMessage(versionMessage)

        verify(peerConnection).close(argThat {
            this is Peer.Error.UnsuitablePeerVersion
        })
    }

    @Test
    fun onReceiveMessage() {
        peer.connected = true
        peer.onMessage(addressMessage)

        verify(listener).onReceiveMessage(peer, addressMessage)
    }
}
