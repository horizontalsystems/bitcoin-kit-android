package io.horizontalsystems.bitcoincore.network.peer

import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.PeerAddress
import io.horizontalsystems.bitcoincore.network.Network
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(PeerAddressManager::class)

class PeerHostManagerTest {
    private val storage = mock(IStorage::class.java)

    private val peerDiscover = mock(PeerDiscover::class.java)
    private val network = mock(Network::class.java)

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
        whenever(storage.getLeastScorePeerAddressExcludingIps(anyList()))
                .thenReturn(
                        PeerAddress(ipsPeers[0]),
                        PeerAddress(ipsPeers[1]))

        peerAddressManager = PeerAddressManager(network, storage)
    }

    @Test
    fun getPeerIp_withoutPeerAddresses() {
        whenever(storage.getLeastScorePeerAddressExcludingIps(anyList()))
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
    fun markSuccess() {
        val peerIp = ipsPeers[0]
        peerAddressManager.markSuccess(peerIp)

        verify(storage).increasePeerAddressScore(peerIp)
    }

    @Test
    fun markFailed() {
        val peerIp = ipsPeers[0]
        peerAddressManager.markFailed(peerIp)

        verify(storage).deletePeerAddress(peerIp)
    }

    @Test
    fun addPeers() {
        val newIps = listOf("2.2.2.2", "2.2.2.2", "3.3.3.3", ipsPeers[0])

        whenever(storage.getExistingPeerAddress(newIps.distinct()))
                .thenReturn(listOf(
                        PeerAddress(ipsPeers[0]),
                        PeerAddress(ipsPeers[1])))

        peerAddressManager.addIps(newIps)

        verify(storage).setPeerAddresses(listOf(
                PeerAddress(ip = newIps[0], score = 0),
                PeerAddress(ip = newIps[2], score = 0)))
    }
}

