package io.horizontalsystems.bitcoincore.network.peer

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.PeerAddress
import io.horizontalsystems.bitcoincore.network.Network
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyList
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(PeerAddressManager::class)

class PeerHostManagerTest {
    private val storage = mock<IStorage>()
    private val peerDiscover = mock<PeerDiscover>()
    private val network = mock<Network>()
    private val listener = mock<PeerGroup>()

    private val ipsPeers = listOf("0.0.0.0", "1.1.1.1")
    private val dnsSeeds = listOf("abc.com", "com.abc")

    private lateinit var peerAddressManager: PeerAddressManager

    @Before
    fun setup() {
        // PeerDiscover
        PowerMockito.whenNew(PeerDiscover::class.java)
                .withAnyArguments()
                .thenReturn(peerDiscover)

        whenever(network.dnsSeeds).thenReturn(dnsSeeds)
        whenever(storage.getLeastScoreFastestPeerAddressExcludingIps(anyList()))
                .thenReturn(
                        PeerAddress(ipsPeers[0]),
                        PeerAddress(ipsPeers[1]))

        peerAddressManager = PeerAddressManager(network, storage)
        peerAddressManager.listener = listener
    }

    @Test
    fun getPeerIp_withoutPeerAddresses() {
        whenever(storage.getLeastScoreFastestPeerAddressExcludingIps(anyList()))
                .thenReturn(null)

        val peerIp = peerAddressManager.getIp()

        verify(peerDiscover).lookup(dnsSeeds)
        assertEquals(null, peerIp)
    }

    @Test
    fun getPeerIp_withPeerAddresses() {
        var peerIp: String?

        // first call
        peerIp = peerAddressManager.getIp()
        assertEquals(ipsPeers[0], peerIp)

        // second call
        peerIp = peerAddressManager.getIp()
        assertEquals(ipsPeers[1], peerIp)

        verifyNoMoreInteractions(peerDiscover)
    }

    @Test
    fun markFailed() {
        val peerIp = ipsPeers[0]
        peerAddressManager.markFailed(peerIp)

        verify(storage).deletePeerAddress(peerIp)
    }

    @Test
    fun addPeers() {
        val newIps = listOf("2.2.2.2", "3.3.3.3")

        peerAddressManager.addIps(newIps)

        verify(listener).onAddAddress()
        verify(storage).setPeerAddresses(listOf(
                PeerAddress(ip = newIps[0], score = 0),
                PeerAddress(ip = newIps[1], score = 0)))
    }
}

