package io.horizontalsystems.bitcoincore.network

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoincore.network.peer.Peer
import io.horizontalsystems.bitcoincore.network.peer.PeerManager
import org.junit.Assert.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PeerManagerTest : Spek({

    val peer1 = mock<Peer> { on { host } doReturn "8.8.8.8" }
    val peer2 = mock<Peer> { on { host } doReturn "9.9.9.9" }
    val peer3 = mock<Peer> { on { host } doReturn "0.0.0.0" }

    val peerManager by memoized { PeerManager() }

    fun addPeer(connected: Boolean, synced: Boolean, host: String = "0.0.0.0"): Peer {
        val peer = mock<Peer> {}

        whenever(peer.connected).thenReturn(connected)
        whenever(peer.synced).thenReturn(synced)
        whenever(peer.host).thenReturn(host)

        peerManager.add(peer)

        return peer
    }

    describe("#add") {
        it("adds peer") {
            peerManager.add(peer1)

            assertEquals(1, peerManager.peersCount())
        }
    }

    describe("#remove") {
        it("remove peer") {
            peerManager.add(peer1)
            assertEquals(1, peerManager.peersCount())

            peerManager.add(peer2)
            assertEquals(2, peerManager.peersCount())

            peerManager.remove(peer1)
            assertEquals(1, peerManager.peersCount())
        }
    }

    describe("disconnectAll") {
        it("disconnects all peers") {
            peerManager.add(peer1)
            peerManager.add(peer2)
            assertEquals(2, peerManager.peersCount())

            peerManager.disconnectAll()
            assertEquals(0, peerManager.peersCount())
        }
    }

    describe("#someReadyPeers") {
        it("gets some ready peers") {
            whenever(peer3.ready).thenReturn(true)

            peerManager.add(peer1)
            peerManager.add(peer2)
            peerManager.add(peer3)
            assertEquals(3, peerManager.peersCount())

            val somePeers = peerManager.someReadyPeers()
            assertEquals(1, somePeers.size)
            assertEquals(peer3.host, somePeers[0].host)
        }
    }

    describe("#connected") {
        it("gets connected peers") {
            peerManager.add(peer1)
            peerManager.add(peer2)
            peerManager.add(peer3)
            assertEquals(listOf<Peer>(), peerManager.connected())

            whenever(peer2.connected).thenReturn(true)
            whenever(peer3.connected).thenReturn(true)
            assertEquals(listOf(peer2, peer3), peerManager.connected())
        }
    }

    describe("isHalfSynced") {
        it("more than half synced") {
            addPeer(host = "0.0.0.1", connected = true, synced = true)
            addPeer(host = "0.0.0.2", connected = true, synced = true)
            addPeer(host = "0.0.0.3", connected = false, synced = false)
            addPeer(host = "0.0.0.4", connected = false, synced = false)

            assertEquals(true, peerManager.isHalfSynced())
        }

        it("less than half synced") {
            addPeer(host = "0.0.0.1", connected = true, synced = true)
            addPeer(host = "0.0.0.2", connected = true, synced = false)
            addPeer(host = "0.0.0.3", connected = false, synced = false)
            addPeer(host = "0.0.0.4", connected = false, synced = true)

            assertEquals(false, peerManager.isHalfSynced())
        }
    }
})
