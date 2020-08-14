package io.horizontalsystems.bitcoincore.transactions

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.blocks.InitialBlockDownload
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.network.peer.Peer
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup
import io.horizontalsystems.bitcoincore.network.peer.PeerManager
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.CopyOnWriteArrayList

object TransactionSenderTest : Spek({

    val transactionSyncer by memoized { mock<TransactionSyncer> {} }
    val peerManager by memoized { mock<PeerManager> {} }
    val initialBlockDownload by memoized { mock<InitialBlockDownload> {} }
    val storage by memoized { mock<IStorage> {} }
    val timer by memoized { mock<TransactionSendTimer> {} }

    val peer1 by memoized { mock<Peer> { on { ready } doReturn true } }
    val peer2 by memoized { mock<Peer> { on { ready } doReturn true } }

    val transactionSender by memoized { TransactionSender(transactionSyncer, peerManager, initialBlockDownload, storage, timer) }

    describe("#canSendTransaction") {

        context("when 0 synced peers") {
            beforeEach {
                whenever(peerManager.peersCount).thenReturn(2)
                whenever(initialBlockDownload.syncedPeers).thenReturn(CopyOnWriteArrayList())
            }

            it("throws an exception") {
                try {
                    transactionSender.canSendTransaction()
                    fail("Expected an Exception to be thrown")
                } catch (e: PeerGroup.Error) {
                    assertTrue(e.message == "peers not synced")
                }
            }
        }

        context("when 2 synced peers and 0 ready peers") {
            beforeEach {
                val syncedPeers = CopyOnWriteArrayList<Peer>().apply {
                    add(peer1)
                    add(peer2)
                }

                whenever(peerManager.peersCount).thenReturn(2)
                whenever(peerManager.readyPears()).thenReturn(emptyList())
                whenever(initialBlockDownload.syncedPeers).thenReturn(syncedPeers)
            }

            it("throws an exception") {
                try {
                    transactionSender.canSendTransaction()
                    fail("Expected an Exception to be thrown")
                } catch (e: PeerGroup.Error) {
                    assertTrue(e.message == "peers not synced")
                }
            }
        }

        context("when 1 ready and 1 synced peers") {
            beforeEach {
                val syncedPeers = CopyOnWriteArrayList<Peer>().apply {
                    add(peer1)
                }

                val readyPeer = mock<Peer> {
                    on { ready } doReturn true
                    on { synced } doReturn true
                }

                whenever(peerManager.peersCount).thenReturn(2)
                whenever(peerManager.readyPears()).thenReturn(listOf(readyPeer))
                whenever(initialBlockDownload.syncedPeers).thenReturn(syncedPeers)
            }

            it("returns nothing") {
                transactionSender.canSendTransaction()
            }
        }

        context("when 1 ready and 2 synced peers") {
            beforeEach {
                val readyPeer = mock<Peer> {
                    on { host } doReturn "0.0.0.1"
                    on { ready } doReturn true
                    on { synced } doReturn true
                }

                val syncedPeer = mock<Peer> {
                    on { host } doReturn "0.0.0.2"
                    on { ready } doReturn false
                    on { synced } doReturn true
                }

                val syncedPeers = CopyOnWriteArrayList<Peer>().apply {
                    add(readyPeer)
                    add(syncedPeer)
                }


                whenever(peerManager.peersCount).thenReturn(2)
                whenever(peerManager.readyPears()).thenReturn(listOf(readyPeer))
                whenever(initialBlockDownload.syncedPeers).thenReturn(syncedPeers)
            }

            it("returns nothing") {
                transactionSender.canSendTransaction()
            }
        }
    }
})
