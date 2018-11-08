package io.horizontalsystems.bitcoinkit.network

import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.verify
import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.messages.AddrMessage
import io.horizontalsystems.bitcoinkit.messages.Message
import io.horizontalsystems.bitcoinkit.messages.VerAckMessage
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

    private val listener = mock(Peer.Listener::class.java)
    private val peerConnection = mock(PeerConnection::class.java)
    private val network = MainNet()

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
        val raw = "F9BEB4D976657273696F6E000000000066000000600C82817F1101000D04000000000000D1DC845B00000000000000000000000000000000000000000000FFFFD4707C069ACD0D040000000000000000000000000000000000000000000000002A13586EB0756F44102F5361746F7368693A302E31362E322FE838080001"
        val msg = getMessageFromHex(raw)

        peer.onMessage(msg)

        argumentCaptor<Message>().apply {
            verify(peerConnection).sendMessage(capture())

            Assert.assertTrue(firstValue is VerAckMessage)
        }
    }

    @Test
    fun onMessage_versionMessage_error_lastBlockIs0() {
        val raw = "F9BEB4D976657273696F6E000000000066000000BE39611E7F1101000D04000000000000D7E5845B00000000000000000000000000000000000000000000FFFFD4707C069DBE0D04000000000000000000000000000000000000000000000000E6030F56C7080373102F5361746F7368693A302E31362E322F0000000001"
        val msg = getMessageFromHex(raw)

        peer.onMessage(msg)

        verify(peerConnection).close(argThat {
            this is Peer.Error.UnsuitablePeerVersion
        })
    }

    @Test
    fun onMessage_versionMessage_error_notFullNode() {
        val raw = "F9BEB4D976657273696F6E00000000006600000041E561B07F110100000000000000000092E4845B00000000000000000000000000000000000000000000FFFFD4707C06B4670D04000000000000000000000000000000000000000000000000E343866042AF517C102F5361746F7368693A302E31362E322FE838080001"
        val msg = getMessageFromHex(raw)

        peer.onMessage(msg)

        verify(peerConnection).close(argThat {
            this is Peer.Error.UnsuitablePeerVersion
        })
    }

    @Test
    fun onMessage_versionMessage_error_notSupportingBloomFilter() {
        val raw = "F9BEB4D976657273696F6E0000000000660000008C490408880D01000D0400000000000061E5845B00000000000000000000000000000000000000000000FFFFD4707C06B26B0D04000000000000000000000000000000000000000000000000D8E6F92E8EC8F039112F5361746F7368693A302E31362E39392FE9380800"
        val msg = getMessageFromHex(raw)

        peer.onMessage(msg)

        verify(peerConnection).close(argThat {
            this is Peer.Error.UnsuitablePeerVersion
        })
    }

    @Test
    fun onReceiveAddresses() {
        val raw = "f9beb4d961646472000000000000000023000000dff105f601250be45b250be45b000000000000000000000000000000000000ffff591ba82e208d"
        val msg = getMessageFromHex(raw) as AddrMessage

        peer.onMessage(msg)
        verify(listener).onReceiveAddress(msg.addresses)
    }

    private fun getMessageFromHex(hex: String): Message {
        val bytes = hex.hexStringToByteArray()
        val input = BitcoinInput(bytes)

        return Message.Builder.parseMessage(input, network)
    }
}
