package bitcoin.wallet.kit.managers

import com.nhaarman.mockito_kotlin.whenever
import helpers.RxTestRule
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.io.FileNotFoundException
import java.net.URL
import java.net.URLConnection

@RunWith(PowerMockRunner::class)
@PrepareForTest(ApiManager::class, URL::class)
class ApiManagerTest {

    private val url = PowerMockito.mock(URL::class.java)
    private val urlConnection = PowerMockito.mock(URLConnection::class.java)

    private lateinit var apiManager: ApiManager

    @Before
    fun setup() {
        RxTestRule.setup()

        PowerMockito
                .whenNew(URL::class.java)
                .withAnyArguments()
                .thenReturn(url)

        whenever(url.openConnection()).thenReturn(urlConnection)

        apiManager = ApiManager("http://ipfs.grouvi.org/ipns/QmVefrf2xrWzGzPpERF6fRHeUTh9uVSyfHHh4cWgUBnXpq/io-hs/data/blockstore")
    }

    @Test
    fun getBlockHashes_onNext() {
        val jsonResponse = "{\"address\":\"mgST3vt11R2HSHqHbtNRgDFdLo8QG2VqzR\",\"blocks\":[{\"hash\":\"3137c5f3db4d59917664371f48bf2a1f5b519063feda0772f64b68a0c51d3d3c\",\"height\":597},{\"hash\":\"717257c058eb9e99c4cf164245d0421d1254a59975369b502cab1abab9d5c8ea\",\"height\":339}]}"
        whenever(urlConnection.getInputStream()).thenReturn(jsonResponse.byteInputStream())

        val address = "mgST3vt11R2HSHqHbtNRgDFdLo8QG2VqzR"
        apiManager.getBlockHashes(address).test()
                .assertValue { blocksList ->
                    blocksList.size == 2 &&
                            blocksList[0].hash == "3137c5f3db4d59917664371f48bf2a1f5b519063feda0772f64b68a0c51d3d3c" &&
                            blocksList[0].height == 597 &&
                            blocksList[1].hash == "717257c058eb9e99c4cf164245d0421d1254a59975369b502cab1abab9d5c8ea" &&
                            blocksList[1].height == 339
                }
    }

    @Test
    fun getBlockHashes_onError() {
        val address = "mgST3vt11R2HSHqHbtNRgDFdLo8QG2VqzR"
        whenever(urlConnection.getInputStream()).thenThrow(FileNotFoundException())
        apiManager.getBlockHashes(address).test()
                .assertValue { blocksList -> blocksList.isEmpty() }
    }

}
