package io.horizontalsystems.bitcoinkit.managers

import com.nhaarman.mockito_kotlin.whenever
import helpers.RxTestRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.io.FileNotFoundException
import java.net.URL
import java.net.URLConnection

@RunWith(PowerMockRunner::class)
@PrepareForTest(ApiManager::class, URL::class)
class ApiManagerTest {

    private val url = mock(URL::class.java)
    private val urlConnection = mock(URLConnection::class.java)

    private lateinit var apiManager: ApiManager

    @Before
    fun setup() {
        RxTestRule.setup()

        PowerMockito
                .whenNew(URL::class.java)
                .withAnyArguments()
                .thenReturn(url)

        whenever(url.openConnection()).thenReturn(urlConnection)

        apiManager = ApiManager("https://ipfs.horizontalsystems.xyz")
    }

    @Test
    fun getJson() {
        val data = "data"
        val resp = "{\"field\":\"$data\"}"

        whenever(urlConnection.getInputStream()).thenReturn(resp.byteInputStream())

        val json = apiManager.getJson("/file.json")
        assertEquals(data, json["field"].asString())
    }

    @Test(expected = FileNotFoundException::class)
    fun getJson_Throws() {
        whenever(urlConnection.getInputStream()).thenThrow(FileNotFoundException())
        apiManager.getJson("/file.json")
    }

}
