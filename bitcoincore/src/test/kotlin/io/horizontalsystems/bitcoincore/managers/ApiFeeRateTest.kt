package io.horizontalsystems.bitcoincore.managers

import com.eclipsesource.json.JsonObject
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoincore.models.FeeRate
import io.horizontalsystems.bitcoincore.RxTestRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(ApiFeeRate::class, ApiManager::class)

class ApiFeeRateTest {
    private val apiManager = mock(ApiManager::class.java)
    private val resource = "ipns/QmXTJZBMMRmBbPun6HFt3tmb3tfYF2usLPxFoacL7G5uMX/blockchain/estimatefee/index.json"

    private val feeRate = FeeRate(
            lowPriority = 10,
            mediumPriority = 20,
            highPriority = 30,
            date = 1543211299660
    )

    private val jsonObject = JsonObject().apply {
        this.add("low_priority", feeRate.lowPriority)
        this.add("medium_priority", feeRate.mediumPriority)
        this.add("high_priority", feeRate.highPriority)
    }

    private lateinit var apiFeeRate: ApiFeeRate

    @Before
    fun setup() {
        RxTestRule.setup()

        PowerMockito
                .whenNew(ApiManager::class.java)
                .withAnyArguments()
                .thenReturn(apiManager)

        whenever(apiManager.getJson(any())).thenReturn(JsonObject().apply {
            add("time", feeRate.date)
            add("rates", JsonObject().apply {
                add("BTC", jsonObject)
                add("BCH", jsonObject)
                add("ETH", jsonObject)
            })
        })
    }

    @Test
    fun getFeeRate() {
        apiFeeRate = ApiFeeRate("BTC")
        apiFeeRate.getFeeRate().test().assertValue {
            feeRate.lowPriority == it.lowPriority &&
                    feeRate.mediumPriority == it.mediumPriority &&
                    feeRate.highPriority == it.highPriority &&
                    feeRate.date == it.date
        }
    }

    @Test
    fun getFeeRate_BTC() {
        assertEquals(1, 1)

        apiFeeRate = ApiFeeRate("BTC")
        apiFeeRate.getFeeRate().test().assertOf {
            verify(apiManager).getJson(resource)
        }
    }

    @Test
    fun getFeeRate_BCH() {
        assertEquals(1, 1)

        apiFeeRate = ApiFeeRate("BCH")
        apiFeeRate.getFeeRate().test().assertOf {
            verify(apiManager).getJson(resource)
        }

    }

}
