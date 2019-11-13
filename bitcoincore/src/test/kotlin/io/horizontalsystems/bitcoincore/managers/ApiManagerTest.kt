package io.horizontalsystems.bitcoincore.managers

import com.eclipsesource.json.JsonObject
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.RxTestRule
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
    fun get() {
        val data = "data"
        val resp = "{\"field\":\"$data\"}"

        whenever(urlConnection.getInputStream()).thenReturn(resp.byteInputStream())

        val json = apiManager.get("/file.json")
        assert(json is JsonObject)
        assertEquals(data, json.asObject()["field"].asString())
    }

    @Test(expected = FileNotFoundException::class)
    fun get_Throws() {
        whenever(urlConnection.getInputStream()).thenThrow(FileNotFoundException())
        apiManager.get("/file.json")
    }

}
