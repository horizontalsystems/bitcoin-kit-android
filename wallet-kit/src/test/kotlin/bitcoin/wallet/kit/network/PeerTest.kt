package bitcoin.wallet.kit.network

import android.content.Context
import bitcoin.wallet.kit.core.hexStringToByteArray
import bitcoin.wallet.kit.messages.Message
import bitcoin.wallet.kit.messages.VerAckMessage
import bitcoin.walllet.kit.io.BitcoinInput
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.verify
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.internal.RealmCore
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(Peer::class, Realm::class, RealmConfiguration::class, RealmCore::class)

class PeerTest {

    private lateinit var peer: Peer
    private lateinit var listener: Peer.Listener
    private lateinit var peerConnection: PeerConnection
    private val network = MainNet()
    private val versionMessage_protocol_69000 = "F9BEB4D976657273696F6E0000000000660000008C490408880D01000D0400000000000061E5845B00000000000000000000000000000000000000000000FFFFD4707C06B26B0D04000000000000000000000000000000000000000000000000D8E6F92E8EC8F039112F5361746F7368693A302E31362E39392FE9380800"
    private val versionMessage_lastBlock_0 = "F9BEB4D976657273696F6E000000000066000000BE39611E7F1101000D04000000000000D7E5845B00000000000000000000000000000000000000000000FFFFD4707C069DBE0D04000000000000000000000000000000000000000000000000E6030F56C7080373102F5361746F7368693A302E31362E322F0000000001"
    private val versionMessage_services_0 = "F9BEB4D976657273696F6E00000000006600000041E561B07F110100000000000000000092E4845B00000000000000000000000000000000000000000000FFFFD4707C06B4670D04000000000000000000000000000000000000000000000000E343866042AF517C102F5361746F7368693A302E31362E322FE838080001"
    private val versionMessage_successful = "F9BEB4D976657273696F6E000000000066000000600C82817F1101000D04000000000000D1DC845B00000000000000000000000000000000000000000000FFFFD4707C069ACD0D040000000000000000000000000000000000000000000000002A13586EB0756F44102F5361746F7368693A302E31362E322FE838080001"

    fun getMessageFromHex(hex: String): Message {
        val versionMessageRaw = hex.hexStringToByteArray()
        val bitcoinInput = BitcoinInput(versionMessageRaw)
        val parsedMsg = Message.Builder.parseMessage<Message>(bitcoinInput, network)
        return parsedMsg
    }

    @Before
    fun setup() {
        listener = mock(Peer.Listener::class.java)
        peerConnection = mock(PeerConnection::class.java)

        PowerMockito
                .whenNew(PeerConnection::class.java)
                .withAnyArguments()
                .thenReturn(peerConnection)

        peer = Peer("host", network, listener)

        // Realm initialize
        PowerMockito.mockStatic(Realm::class.java)
        PowerMockito.mockStatic(RealmConfiguration::class.java)
        PowerMockito.mockStatic(RealmCore::class.java)

        RealmCore.loadLibrary(Mockito.any(Context::class.java))
    }

    @Test
    fun onMessage_versionMessage_success() {
        val versionMessage = getMessageFromHex(versionMessage_successful)

        peer.onMessage(versionMessage)

        argumentCaptor<Message>().apply {
            verify(peerConnection).sendMessage(capture())

            Assert.assertTrue(firstValue is VerAckMessage)
        }
    }

    @Test
    fun onMessage_versionMessage_error_lastBlockIs0() {
        val versionMessage = getMessageFromHex(versionMessage_lastBlock_0)

        peer.onMessage(versionMessage)

        verify(peerConnection).close()
    }

    @Test
    fun onMessage_versionMessage_error_notFullNode() {
        val versionMessage = getMessageFromHex(versionMessage_services_0)

        peer.onMessage(versionMessage)

        verify(peerConnection).close()
    }

    @Test
    fun onMessage_versionMessage_error_notSupportingBloomFilter() {
        val versionMessage = getMessageFromHex(versionMessage_protocol_69000)

        peer.onMessage(versionMessage)

        verify(peerConnection).close()
    }

}
