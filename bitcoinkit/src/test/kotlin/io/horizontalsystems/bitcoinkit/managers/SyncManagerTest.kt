package io.horizontalsystems.bitcoinkit.managers

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.network.peer.PeerGroup
import io.reactivex.subjects.PublishSubject
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class SyncManagerTest : Spek({
    lateinit var syncManager: SyncManager
    lateinit var timer: PublishSubject<Long>

    val connectionManager = mock(ConnectionManager::class.java)
    val feeRateSyncer = mock(FeeRateSyncer::class.java)
    val peerGroup = mock(PeerGroup::class.java)
    val initialSyncer = mock(InitialSyncer::class.java)

    beforeEachTest {
        timer = PublishSubject.create()
        syncManager = SyncManager(connectionManager, feeRateSyncer, peerGroup, initialSyncer, timer)
    }

    afterEachTest {
        reset(connectionManager, feeRateSyncer, peerGroup, initialSyncer)
    }

    describe("#start") {
        beforeEach {
            whenever(connectionManager.isOnline).thenReturn(true)
        }

        it("starts :initialSyncer") {
            syncManager.start()

            verify(initialSyncer).sync()
        }

        it("starts :feeRateSyncer by timer") {
            syncManager.start()
            timer.onNext(1)

            verify(feeRateSyncer).sync()
        }

        context("when no internet connection") {
            beforeEach {
                whenever(connectionManager.isOnline).thenReturn(false)
            }

            it("does not sync fee rate") {
                syncManager.start()
                timer.onNext(1)

                verifyNoMoreInteractions(feeRateSyncer)
            }
        }
    }

    describe("#stop") {
        it("stops :peerGroup") {
            syncManager.stop()

            verify(peerGroup).close()
        }

        it("stops :initialSyncer") {
            syncManager.stop()

            verify(peerGroup).close()
        }
    }

    describe("#onSyncingFinished") {
        it("starts peer group") {
            syncManager.onSyncingFinished()

            verify(peerGroup).start()
        }
    }

})
