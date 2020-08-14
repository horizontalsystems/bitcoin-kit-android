package io.horizontalsystems.bitcoincore.network

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.network.peer.Peer
import io.horizontalsystems.bitcoincore.network.peer.PeerManager
import org.junit.Assert.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PeerManagerTest : Spek({

    val peer1 by memoized { mock<Peer> { on { host } doReturn "8.8.8.8" } }
    val peer2 by memoized { mock<Peer> { on { host } doReturn "9.9.9.9" } }
    val peer3 by memoized { mock<Peer> { on { host } doReturn "0.0.0.0" } }

    val peerManager by memoized { PeerManager() }

    fun addPeer(synced: Boolean, connected: Boolean = true, host: String = "0.0.0.0", ready: Boolean = true): Peer {
        val peer = mock<Peer> {}

        whenever(peer.connected).thenReturn(connected)
        whenever(peer.synced).thenReturn(synced)
        whenever(peer.host).thenReturn(host)
        whenever(peer.ready).thenReturn(ready)

        peerManager.add(peer)

        return peer
    }

    describe("#add") {
        it("adds peer") {
            peerManager.add(peer1)

            assertEquals(1, peerManager.peersCount)
        }
    }

    describe("#remove") {
        it("remove peer") {
            peerManager.add(peer1)
            assertEquals(1, peerManager.peersCount)

            peerManager.add(peer2)
            assertEquals(2, peerManager.peersCount)

            peerManager.remove(peer1)
            assertEquals(1, peerManager.peersCount)
        }
    }

    describe("disconnectAll") {
        it("disconnects all peers") {
            peerManager.add(peer1)
            peerManager.add(peer2)
            assertEquals(2, peerManager.peersCount)

            peerManager.disconnectAll()
            assertEquals(0, peerManager.peersCount)
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

    describe("#sorted") {
        it("gets connected peers sorted by connection time") {
            peerManager.add(peer1)
            peerManager.add(peer2)
            peerManager.add(peer3)
            assertEquals(listOf<Peer>(), peerManager.sorted())

            whenever(peer2.connected).thenReturn(true)
            whenever(peer2.connectionTime).thenReturn(102)
            whenever(peer3.connected).thenReturn(true)
            whenever(peer3.connectionTime).thenReturn(100)

            assertEquals(listOf(peer3, peer2), peerManager.sorted())
        }
    }

    describe("#readyPears") {
        it("returns a list of ready peers") {
            addPeer(host = "0.0.0.1", connected = true, synced = true, ready = false)
            addPeer(host = "0.0.0.2", connected = true, synced = true, ready = false)
            val p3 = addPeer(host = "0.0.0.3", connected = true, synced = true, ready = true)
            val p4 = addPeer(host = "0.0.0.4", connected = true, synced = true, ready = true)

            assertEquals(listOf(p3, p4), peerManager.readyPears())
        }

        it("returns an empty list") {
            addPeer(host = "0.0.0.1", connected = true, synced = true, ready = false)
            addPeer(host = "0.0.0.2", connected = true, synced = true, ready = false)

            assertEquals(listOf<Peer>(), peerManager.readyPears())
        }
    }

    describe("hasSyncedPeer") {
        it("is true when at least one peer is synced") {
            addPeer(host = "0.0.0.1", connected = true, synced = true)
            addPeer(host = "0.0.0.2", connected = true, synced = true)
            addPeer(host = "0.0.0.3", connected = false, synced = false)
            addPeer(host = "0.0.0.4", connected = false, synced = false)

            assertEquals(true, peerManager.hasSyncedPeer())
        }

        it("is false when no peer is synced") {
            addPeer(host = "0.0.0.1", connected = true, synced = false)
            addPeer(host = "0.0.0.2", connected = true, synced = false)
            addPeer(host = "0.0.0.3", connected = false, synced = false)
            addPeer(host = "0.0.0.4", connected = false, synced = false)

            assertEquals(false, peerManager.hasSyncedPeer())
        }
    }
})
