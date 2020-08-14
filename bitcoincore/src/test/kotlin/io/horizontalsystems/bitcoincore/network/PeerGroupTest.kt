//package io.horizontalsystems.bitcoincore.network
//
//import com.nhaarman.mockitokotlin2.whenever
//import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
//import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
//import io.horizontalsystems.bitcoincore.managers.ConnectionManager
//import io.horizontalsystems.bitcoincore.models.NetworkAddress
//import io.horizontalsystems.bitcoincore.network.messages.AddrMessage
//import io.horizontalsystems.bitcoincore.network.messages.NetworkMessageParser
//import io.horizontalsystems.bitcoincore.network.messages.NetworkMessageSerializer
//import io.horizontalsystems.bitcoincore.network.peer.Peer
//import io.horizontalsystems.bitcoincore.network.peer.PeerAddressManager
//import io.horizontalsystems.bitcoincore.network.peer.PeerGroup
//import io.horizontalsystems.bitcoincore.network.peer.PeerManager
//import io.horizontalsystems.bitcoincore.network.peer.task.SendTransactionTask
//import org.junit.Before
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.mockito.Mockito.mock
//import org.mockito.Mockito.verify
//import org.powermock.api.mockito.PowerMockito
//import org.powermock.core.classloader.annotations.PrepareForTest
//import org.powermock.modules.junit4.PowerMockRunner
//import java.net.SocketTimeoutException
//
//@RunWith(PowerMockRunner::class)
//@PrepareForTest(PeerGroup::class)
//
//class PeerGroupTest {
//
//    private var peer1 = mock(Peer::class.java)
//    private var peer2 = mock(Peer::class.java)
//    private var hostManager = mock(PeerAddressManager::class.java)
//    private var peerManager = mock(PeerManager::class.java)
//    private var connectionManager = mock(ConnectionManager::class.java)
//    private var relayTransactionTask = mock(SendTransactionTask::class.java)
//    private val networkMessageParser = mock(NetworkMessageParser::class.java)
//    private val networkMessageSerializer = mock(NetworkMessageSerializer::class.java)
//
//    private val peerIp = "8.8.8.8"
//    private val peerIp2 = "5.5.5.5"
//    private val network = mock(Network::class.java)
//
//    private lateinit var peerGroup: PeerGroup
//
//    @Before
//    fun setup() {
//        whenever(peer1.host).thenReturn(peerIp)
//        whenever(peer2.host).thenReturn(peerIp2)
//        whenever(hostManager.getIp()).thenReturn(peerIp, peerIp2)
//        whenever(connectionManager.isConnected).thenReturn(true)
//
//        // Peer
//        PowerMockito.whenNew(Peer::class.java)
//                .withAnyArguments()
//                .thenReturn(peer1, peer2)
//
//        // RelayTransactionTask
//        PowerMockito.whenNew(SendTransactionTask::class.java)
//                .withAnyArguments()
//                .thenReturn(relayTransactionTask)
//
//        peerGroup = PeerGroup(hostManager, network, peerManager, 2, networkMessageParser, networkMessageSerializer, connectionManager, 100)
//    }
//
//    @Test
//    fun start() { // creates peer connection with given IP address
//        peerGroup.start()
//
//        verify(peer1).start()
//    }
//
//    @Test
//    fun disconnected_withError() { // removes peer from connection list
//        peerGroup.onDisconnect(peer1, SocketTimeoutException("Some Error"))
//
//        verify(hostManager).markFailed(peerIp)
//    }
//
//    @Test
//    fun onReceiveMessage() {
//        val ip4 = "0A000001"
//        val raw = arrayOf("E215104D", "0100000000000000", "00000000000000000000FFFF$ip4", "208D").joinToString("")
//        val input = BitcoinInputMarkable(raw.hexToByteArray())
//
//        val netAddress = NetworkAddress(input, false)
//
//        peerGroup.onReceiveMessage(peer1, AddrMessage(listOf(netAddress)))
//
//        verify(hostManager).addIps(listOf("10.0.0.1"))
//    }
//
//}
