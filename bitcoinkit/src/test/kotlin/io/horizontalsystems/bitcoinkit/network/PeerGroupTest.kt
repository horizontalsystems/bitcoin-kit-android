package io.horizontalsystems.bitcoinkit.network

import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.core.KitStateProvider
import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.horizontalsystems.bitcoinkit.managers.ConnectionManager
import io.horizontalsystems.bitcoinkit.models.NetworkAddress
import io.horizontalsystems.bitcoinkit.network.peer.Peer
import io.horizontalsystems.bitcoinkit.network.peer.PeerGroup
import io.horizontalsystems.bitcoinkit.network.peer.PeerHostManager
import io.horizontalsystems.bitcoinkit.network.peer.PeerManager
import io.horizontalsystems.bitcoinkit.network.peer.task.SendTransactionTask
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.net.SocketTimeoutException

@RunWith(PowerMockRunner::class)
@PrepareForTest(PeerGroup::class)

class PeerGroupTest {

    private var peer1 = mock(Peer::class.java)
    private var peer2 = mock(Peer::class.java)
    private var hostManager = mock(PeerHostManager::class.java)
    private var peerManager = mock(PeerManager::class.java)
    private var bloomFilterManager = mock(BloomFilterManager::class.java)
    private var kitStateProvider = mock(KitStateProvider::class.java)
    private var connectionManager = mock(ConnectionManager::class.java)
    private var relayTransactionTask = mock(SendTransactionTask::class.java)

    private val peerIp = "8.8.8.8"
    private val peerIp2 = "5.5.5.5"
    private val network = MainNet()

    private lateinit var peerGroup: PeerGroup

    @Before
    fun setup() {
        whenever(peer1.host).thenReturn(peerIp)
        whenever(peer2.host).thenReturn(peerIp2)
        whenever(hostManager.getPeerIp()).thenReturn(peerIp, peerIp2)
        whenever(connectionManager.isOnline).thenReturn(true)

        // Peer
        PowerMockito.whenNew(Peer::class.java)
                .withAnyArguments()
                .thenReturn(peer1, peer2)

        // PeerManager
        PowerMockito.whenNew(PeerManager::class.java)
                .withAnyArguments()
                .thenReturn(peerManager)

        // RelayTransactionTask
        PowerMockito.whenNew(SendTransactionTask::class.java)
                .withAnyArguments()
                .thenReturn(relayTransactionTask)

        peerGroup = PeerGroup(hostManager, bloomFilterManager, network, kitStateProvider, 2)
        peerGroup.connectionManager = connectionManager
    }

    @Test
    fun run() { // creates peer connection with given IP address
        peerGroup.start()

        Thread.sleep(500L)
        verify(peer1).start()

        // close thread:
        peerGroup.close()
        peerGroup.join()
    }


    @Test
    fun disconnected_withError() { // removes peer from connection list
        peerGroup.onDisconnect(peer1, SocketTimeoutException("Some Error"))

        verify(hostManager).markFailed(peerIp)
    }

//    @Test
//    fun sendPendingTransactions() { // send transaction
//        whenever(peer1.ready).thenReturn(true)
//        peerGroup.onConnect(peer1)
//
//        val transaction = Transaction()
//        peerGroup.sendPendingTransactions(transaction)
//
//        Thread.sleep(100) // wait thread executor
//        verify(peer1).addTask(relayTransactionTask)
//    }

    @Test
    fun onReceiveAddresses() {
        val ip4 = "0A000001"
        val raw = arrayOf("E215104D", "0100000000000000", "00000000000000000000FFFF$ip4", "208D").joinToString("")
        val input = BitcoinInput(raw.hexStringToByteArray())

        val netAddress = NetworkAddress(input, false)

        peerGroup.onReceiveAddress(arrayOf(netAddress))

        verify(hostManager).addPeers(arrayOf("10.0.0.1"))
    }

    @Test
    fun sendPendingTransactions_noPeers() {
        try {
            peerGroup.sendPendingTransactions()
            fail("Expected exception PeerGroup.Error")
        } catch (e: PeerGroup.Error) {
            assertEquals("No peers connected", e.message)
        }
    }

    @Test
    fun sendPendingTransactions_notSynced() {
        try {
            whenever(peerManager.peersCount()).thenReturn(1)
            whenever(peerManager.nonSyncedPeer()).thenReturn(peer1)

            peerGroup.sendPendingTransactions()
            fail("Expected exception PeerGroup.Error")
        } catch (e: PeerGroup.Error) {
            assertEquals("Peers not synced yet", e.message)
        }
    }
}
