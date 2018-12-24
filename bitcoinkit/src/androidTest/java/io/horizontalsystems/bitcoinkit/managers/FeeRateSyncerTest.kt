package io.horizontalsystems.bitcoinkit.managers

import com.nhaarman.mockito_kotlin.whenever
import helpers.RxTestRule
import io.horizontalsystems.bitcoinkit.RealmFactoryMock
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.realm.Realm
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class FeeRateSyncerTest {
    private val factory = RealmFactoryMock()
    private val apiFeeRate = mock(ApiFeeRate::class.java)
    private val connectionManager = mock(ConnectionManager::class.java)

    private lateinit var realm: Realm
    private lateinit var timer: PublishSubject<Long>
    private lateinit var feeRateSyncer: FeeRateSyncer

    @Before
    fun setUp() {
        RxTestRule.setup()

        realm = factory.realmFactory.realm
        realm.executeTransaction { it.deleteAll() }

        whenever(apiFeeRate.getFeeRate()).thenReturn(Observable.empty())
        whenever(connectionManager.isOnline).thenReturn(true)
    }

    @Test
    fun start_getRateOnce() {
        timer = PublishSubject.create()
        feeRateSyncer = FeeRateSyncer(factory.realmFactory, apiFeeRate, timer, connectionManager)
        feeRateSyncer.start()
        timer.onNext(1)

        verify(apiFeeRate).getFeeRate()
    }

    @Test
    fun start_getRateTwice() {
        timer = PublishSubject.create()
        feeRateSyncer = FeeRateSyncer(factory.realmFactory, apiFeeRate, timer, connectionManager)
        feeRateSyncer.start()
        timer.onNext(1)
        timer.onNext(1)

        verify(apiFeeRate, times(2)).getFeeRate()
    }

    @Test
    fun start_getRateNone() {
        timer = PublishSubject.create()

        feeRateSyncer = FeeRateSyncer(factory.realmFactory, apiFeeRate, timer, connectionManager)
        feeRateSyncer.start()

        verifyNoMoreInteractions(apiFeeRate)
    }

    @Test
    fun stop_cancelTimer() {
        timer = PublishSubject.create()

        feeRateSyncer = FeeRateSyncer(factory.realmFactory, apiFeeRate, timer, connectionManager)
        feeRateSyncer.start()
        feeRateSyncer.stop()
        timer.onNext(1)

        verifyNoMoreInteractions(apiFeeRate)
    }

    @Test
    fun start_refresh() {
        timer = PublishSubject.create()
        feeRateSyncer = FeeRateSyncer(factory.realmFactory, apiFeeRate, timer, connectionManager)
        feeRateSyncer.start()
        feeRateSyncer.start()
        timer.onNext(1)

        verify(apiFeeRate).getFeeRate()
    }
}
