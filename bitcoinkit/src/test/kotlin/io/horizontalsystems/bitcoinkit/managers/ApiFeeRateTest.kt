package io.horizontalsystems.bitcoinkit.managers

import com.eclipsesource.json.JsonObject
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import helpers.RxTestRule
import io.horizontalsystems.bitcoinkit.BitcoinKit.NetworkType
import io.horizontalsystems.bitcoinkit.models.FeeRate
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(ApiFeeRate::class, ApiManager::class)

class ApiFeeRateTest {
    private val apiManager = mock(ApiManager::class.java)

    private val feeRate = FeeRate().apply {
        lowPriority = 1.0
        mediumPriority = 3.0
        highPriority = 12.0
        dateStr = "2018-11-27 09:55"
        date = 1543312547801
    }

    private val coinJsonObject = JsonObject().apply {
        this.add("low_priority", feeRate.lowPriority.toInt())
        this.add("medium_priority", feeRate.mediumPriority.toInt())
        this.add("high_priority", feeRate.highPriority.toInt())
    }

    private val btcJsonObject = JsonObject().add("BTC", coinJsonObject)
    private val jsonObject = JsonObject().apply {
        this.add("rates", btcJsonObject)
        this.add("time", feeRate.date)
        this.add("time_str", feeRate.dateStr)
    }

    private lateinit var apiFeeRate: ApiFeeRate

    @Before
    fun setup() {
        RxTestRule.setup()

        PowerMockito
                .whenNew(ApiManager::class.java)
                .withAnyArguments()
                .thenReturn(apiManager)

        whenever(apiManager.getJson(any())).thenReturn(jsonObject)
    }

    @Test
    fun getFeeRate() {
        apiFeeRate = ApiFeeRate(NetworkType.MainNet)
        apiFeeRate.getFeeRate().test().assertValue {
            feeRate.lowPriority == it.lowPriority &&
                    feeRate.mediumPriority == it.mediumPriority &&
                    feeRate.highPriority == it.highPriority &&
                    feeRate.date == it.date &&
                    feeRate.dateStr == it.dateStr
        }
    }

}
