package io.horizontalsystems.bitcoinkit.managers

import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.hdwalletkit.HDPublicKey
import io.horizontalsystems.hdwalletkit.HDWallet
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(InitialSyncerApi::class)

class InitialSyncerApiTest {

    private val wallet = mock(HDWallet::class.java)
    private val apiManager = mock(ApiManager::class.java)
    private val network = mock(Network::class.java)
    private val checkpointBlock = mock(Block::class.java)
    private val addressSelector = mock(IAddressSelector::class.java)

    private lateinit var initialSyncerApi: InitialSyncerApi

    @Before
    fun setup() {
        whenever(wallet.gapLimit).thenReturn(1)

        whenever(network.checkpointBlock).thenReturn(checkpointBlock)
        whenever(checkpointBlock.height).thenReturn(1000)
        whenever(addressSelector.getAddressVariants(any())).thenReturn(
                listOf("1A282zR9uMz84P9vQNwCBCqfwGtKEu3K3v", "37QxhC4rF4nuJgikxY2MdKRupnXjomyuoU", "a0"))

        whenever(wallet.hdPublicKey(0, true)).thenReturn(getPubKey(1))
        whenever(wallet.hdPublicKey(1, true)).thenReturn(getPubKey(2))
        whenever(wallet.hdPublicKey(2, true)).thenReturn(getPubKey(3))

        PowerMockito
                .whenNew(ApiManager::class.java)
                .withAnyArguments()
                .thenReturn(apiManager)

        initialSyncerApi = InitialSyncerApi(wallet, addressSelector, network)
    }

    @Test
    fun fetchFromApi() {
        initialSyncerApi.fetchFromApi(true).test()

        verify(apiManager).getJsonArray("tx/address/1A282zR9uMz84P9vQNwCBCqfwGtKEu3K3v")
    }

    @Test
    fun fetchFromApi_handleResponse() {
        val obj1 = JsonObject().add("block", "001").add("height", 1)
        val obj2 = JsonObject().add("block", "002").add("height", 2)

        whenever(apiManager.getJsonArray(any()))
                .thenReturn(
                        JsonArray().apply { add(obj1).add(obj2) },
                        JsonArray() // response for second call
                )

        initialSyncerApi.fetchFromApi(true)
                .test()
                .assertValue {
                    val keys = it.first
                    keys.any { key ->
                        key.publicKey.contentEquals(byteArrayOf(1))
                    } && keys.any { key ->
                        key.publicKey.contentEquals(byteArrayOf(2))
                    }
                }
                .assertValue {
                    val blocks = it.second
                    blocks.any { block ->
                        block.height == 1
                    } && blocks.any { block ->
                        block.height == 2
                    }
                }
    }

    private fun getPubKey(i: Int): HDPublicKey {
        return HDPublicKey().apply {
            index = i
            external = true
            publicKey = byteArrayOf(i.toByte())
            publicKeyHash = "a$i".hexStringToByteArray()
        }
    }
}
