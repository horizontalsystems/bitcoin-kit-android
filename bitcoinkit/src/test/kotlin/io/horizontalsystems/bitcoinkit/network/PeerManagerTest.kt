package io.horizontalsystems.bitcoinkit.network

import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class PeerManagerTest {
    private val peer1 = mock(Peer::class.java)
    private val peer2 = mock(Peer::class.java)
    private val peer3 = mock(Peer::class.java)

    private lateinit var peerManager: PeerManager

    @Before
    fun setup() {
        whenever(peer1.host).thenReturn("8.8.8.8")
        whenever(peer2.host).thenReturn("9.9.9.9")
        whenever(peer3.host).thenReturn("0.0.0.0")

        peerManager = PeerManager()
    }

    @Test
    fun add() {
        peerManager.add(peer1)
        assertEquals(1, peerManager.peersCount())
    }

    @Test
    fun remove() {
        peerManager.add(peer1)
        assertEquals(1, peerManager.peersCount())

        peerManager.add(peer2)
        assertEquals(2, peerManager.peersCount())

        peerManager.remove(peer1)
        assertEquals(1, peerManager.peersCount())
    }

    @Test
    fun disconnectAll() {
        peerManager.add(peer1)
        peerManager.add(peer2)
        assertEquals(2, peerManager.peersCount())

        peerManager.disconnectAll()
        assertEquals(0, peerManager.peersCount())
    }

    @Test
    fun someReadyPeers() {
        whenever(peer3.ready).thenReturn(true)

        peerManager.add(peer1)
        peerManager.add(peer2)
        peerManager.add(peer3)
        assertEquals(3, peerManager.peersCount())

        val somePeers = peerManager.someReadyPeers()
        assertEquals(1, somePeers.size)
        assertEquals(peer3.host, somePeers[0].host)
    }

    @Test
    fun connected() {
        peerManager.add(peer1)
        peerManager.add(peer2)
        peerManager.add(peer3)
        assertEquals(listOf<Peer>(), peerManager.connected())

        whenever(peer2.connected).thenReturn(true)
        whenever(peer3.connected).thenReturn(true)
        assertEquals(listOf(peer2, peer3), peerManager.connected())
    }

    @Test
    fun nonSyncedPeer() {
        peerManager.add(peer1)
        peerManager.add(peer2)
        peerManager.add(peer3)

        assertEquals(null, peerManager.nonSyncedPeer())

        whenever(peer1.synced).thenReturn(false)
        whenever(peer1.connected).thenReturn(true)
        assertEquals(peer1, peerManager.nonSyncedPeer())
    }

    @Test
    fun isSyncPeer() {
        assertFalse(peerManager.isSyncPeer(peer1))

        peerManager.syncPeer = peer1
        assertTrue(peerManager.isSyncPeer(peer1))
    }
}
