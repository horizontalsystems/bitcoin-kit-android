package io.horizontalsystems.bitcoinkit.network

import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.network.messages.AddrMessage
import io.horizontalsystems.bitcoinkit.network.messages.Message
import io.horizontalsystems.bitcoinkit.network.messages.VerAckMessage
import io.horizontalsystems.bitcoinkit.network.messages.VersionMessage
import io.horizontalsystems.bitcoinkit.network.peer.Peer
import io.horizontalsystems.bitcoinkit.network.peer.PeerConnection
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(Peer::class)

class PeerTest {

    private val storage = mock(IStorage::class.java)
    private val listener = mock(Peer.Listener::class.java)
    private val peerConnection = mock(PeerConnection::class.java)
    private val network = MainNet(storage)
    private val versionMessage = mock(VersionMessage::class.java)
    private val addressMessage = mock(AddrMessage::class.java)

    private lateinit var peer: Peer

    @Before
    fun setup() {
        PowerMockito
                .whenNew(PeerConnection::class.java)
                .withAnyArguments()
                .thenReturn(peerConnection)

        peer = Peer("host", network, listener)
    }

    @Test
    fun onMessage_versionMessage_success() {
        whenever(versionMessage.lastBlock).thenReturn(99)
        whenever(versionMessage.hasBlockChain(network)).thenReturn(true)
        whenever(versionMessage.supportsBloomFilter(network)).thenReturn(true)

        peer.onMessage(versionMessage)

        argumentCaptor<Message>().apply {
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
    fun onReceiveAddresses() {
        peer.connected = true
        peer.onMessage(addressMessage)
        verify(listener).onReceiveAddress(addressMessage.addresses)
    }
}
