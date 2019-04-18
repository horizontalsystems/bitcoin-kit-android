package io.horizontalsystems.bitcoincore.managers

import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.RxTestRule
import io.reactivex.Maybe
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify

class FeeRateSyncerTest {
    private val storage = Mockito.mock(IStorage::class.java)
    private val apiFeeRate = Mockito.mock(ApiFeeRate::class.java)

    private lateinit var feeRateSyncer: FeeRateSyncer

    @Before
    fun setUp() {
        RxTestRule.setup()
    }

    @Test
    fun sync() {
        whenever(apiFeeRate.getFeeRate()).thenReturn(Maybe.empty())

        feeRateSyncer = FeeRateSyncer(storage, apiFeeRate)
        feeRateSyncer.sync()

        verify(apiFeeRate).getFeeRate()
    }

}

