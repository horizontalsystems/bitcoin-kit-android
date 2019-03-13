package io.horizontalsystems.bitcoinkit.managers

import com.eclipsesource.json.JsonObject
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import helpers.RxTestRule
import io.horizontalsystems.bitcoinkit.BitcoinKit.NetworkType
import io.horizontalsystems.bitcoinkit.models.FeeRate
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
    private val resource = "ipns/Qmd4Gv2YVPqs6dmSy1XEq7pQRSgLihqYKL2JjK7DMUFPVz/io-hs/data/blockchain"

    private val feeRate = FeeRate(
            lowPriority = "0.00001023",
            mediumPriority = "0.00001023",
            highPriority = "0.00001023",
            date = 1543211299660
    )

    private val jsonObject = JsonObject().apply {
        this.add("low_priority", feeRate.lowPriority)
        this.add("medium_priority", feeRate.mediumPriority)
        this.add("high_priority", feeRate.highPriority)
        this.add("date", feeRate.date)
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
                    feeRate.date == it.date
        }
    }

    @Test
    fun getFeeRate_BTC() {
        assertEquals(1, 1)

        // MainNet
        apiFeeRate = ApiFeeRate(NetworkType.MainNet)
        apiFeeRate.getFeeRate().test().assertOf {
            verify(apiManager).getJson("$resource/BTC/estimatefee/index.json")
        }

        // TestNet
        apiFeeRate = ApiFeeRate(NetworkType.TestNet)
        apiFeeRate.getFeeRate().test().assertOf {
            verify(apiManager).getJson("$resource/BTC/testnet/estimatefee/index.json")
        }

        // RegNet
        apiFeeRate = ApiFeeRate(NetworkType.RegTest)
        apiFeeRate.getFeeRate().test().assertOf {
            verify(apiManager).getJson("$resource/BTC/regtest/estimatefee/index.json")
        }
    }

    @Test
    fun getFeeRate_BCH() {
        assertEquals(1, 1)

        // MainNet
        apiFeeRate = ApiFeeRate(NetworkType.MainNetBitCash)
        apiFeeRate.getFeeRate().test().assertOf {
            verify(apiManager).getJson("$resource/BCH/estimatefee/index.json")
        }

        // TestNet
        apiFeeRate = ApiFeeRate(NetworkType.TestNetBitCash)
        apiFeeRate.getFeeRate().test().assertOf {
            verify(apiManager).getJson("$resource/BCH/estimatefee/index.json")
        }
    }

}
