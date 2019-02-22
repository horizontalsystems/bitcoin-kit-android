package io.horizontalsystems.bitcoinkit.managers

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.PublicKey
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class InitialSyncerApiBatchTest {
    private val wallet = Mockito.mock(Wallet::class.java)
    private val apiManager = Mockito.mock(ApiManager::class.java)
    private val addressSelector = Mockito.mock(IAddressSelector::class.java)
    private val xxx = Mockito.mock(BlockHashFetcher::class.java)

    private lateinit var syncerApi: InitialSyncerApiBatch
    private val gapLimit = 3

    @Before
    fun setup() {
        whenever(wallet.gapLimit).thenReturn(gapLimit)

//        whenever(network.checkpointBlock).thenReturn(checkpointBlock)
//        whenever(checkpointBlock.height).thenReturn(1000)
//        whenever(addressSelector.getAddressVariants(any())).thenReturn(
//                listOf("1A282zR9uMz84P9vQNwCBCqfwGtKEu3K3v", "37QxhC4rF4nuJgikxY2MdKRupnXjomyuoU", "a0"))
//
//        whenever(wallet.hdPublicKey(account, 0, true)).thenReturn(getPubKey(1))
//        whenever(wallet.hdPublicKey(account, 1, true)).thenReturn(getPubKey(2))
//        whenever(wallet.hdPublicKey(account, 2, true)).thenReturn(getPubKey(3))

//        PowerMockito
//                .whenNew(ApiManager::class.java)
//                .withAnyArguments()
//                .thenReturn(apiManager)

        syncerApi = InitialSyncerApiBatch(wallet, xxx)
    }

    @Test
    fun fetchFromApi() {
        val account = 0
        val external = true

        val publicKey0 = mock<PublicKey>()
        val publicKey1 = mock<PublicKey>()
        val publicKey2 = mock<PublicKey>()
        val publicKey3 = mock<PublicKey>()
        val publicKey4 = mock<PublicKey>()
        val publicKeysCycle1 = listOf(publicKey0, publicKey1, publicKey2)
        val publicKeysCycle2 = listOf(publicKey3, publicKey4)
        val lastUsedPublicKeyIndex = 1

        whenever(wallet.publicKey(account, 0, external)).thenReturn(publicKey0)
        whenever(wallet.publicKey(account, 1, external)).thenReturn(publicKey1)
        whenever(wallet.publicKey(account, 2, external)).thenReturn(publicKey2)

        val blockHash = mock<BlockHash>()

        whenever(xxx.getBlockHashes(publicKeysCycle1)).thenReturn(Pair(listOf(blockHash), lastUsedPublicKeyIndex))

        whenever(wallet.publicKey(account, 3, external)).thenReturn(publicKey3)
        whenever(wallet.publicKey(account, 4, external)).thenReturn(publicKey4)

        whenever(xxx.getBlockHashes(publicKeysCycle2)).thenReturn(Pair(listOf(), -1))

        syncerApi.fetchFromApi(account, external)
                .test()
                .assertValue(Pair(publicKeysCycle1 + publicKeysCycle2, listOf(blockHash)))

    }


//    @Test
//    fun fetchFromApi_empty() {
//        val account = 0
//        val external = true
//
//        val publicKey0 = mock(PublicKey::class.java)
//        val publicKey1 = mock(PublicKey::class.java)
//        val publicKey2 = mock(PublicKey::class.java)
//
//        val addresses0 = listOf("0_0", "0_1")
//        val addresses1 = listOf("1_0", "1_1")
//        val addresses2 = listOf("2_0", "2_1")
//
//        val addresses = addresses0 + addresses1 + addresses2
//
//        whenever(wallet.publicKey(account, 0, external)).thenReturn(publicKey0)
//        whenever(wallet.publicKey(account, 1, external)).thenReturn(publicKey1)
//        whenever(wallet.publicKey(account, 2, external)).thenReturn(publicKey2)
//
//        whenever(addressSelector.getAddressVariants(publicKey0)).thenReturn(addresses0)
//        whenever(addressSelector.getAddressVariants(publicKey1)).thenReturn(addresses1)
//        whenever(addressSelector.getAddressVariants(publicKey2)).thenReturn(addresses2)
//
//        whenever(apiManager.getTransactions(addresses)).thenReturn(listOf())
//
//        syncerApi.fetchFromApi(account, external).test()
//                .assertValue(Pair(listOf(publicKey0, publicKey1, publicKey2), listOf()))
//
//        verify(apiManager).getTransactions(addresses)
//    }

//    @Test
//    fun fetchFromApi() {
//        val account = 0
//        val external = true
//
//        val publicKey0 = mock(PublicKey::class.java)
//        val publicKey1 = mock(PublicKey::class.java)
//        val publicKey2 = mock(PublicKey::class.java)
//        val publicKey3 = mock(PublicKey::class.java)
//        val publicKey4 = mock(PublicKey::class.java)
//
//        val addresses0 = listOf("0_0", "0_1")
//        val addresses1 = listOf("1_0", "1_1")
//        val addresses2 = listOf("2_0", "2_1")
//        val addresses3 = listOf("3_0", "3_1")
//        val addresses4 = listOf("4_0", "4_1")
//
//        val addressesForCycle1 = addresses0 + addresses1 + addresses2
//        val addressesForCycle2 = addresses3 + addresses4
//
//        whenever(wallet.publicKey(account, 0, external)).thenReturn(publicKey0)
//        whenever(wallet.publicKey(account, 1, external)).thenReturn(publicKey1)
//        whenever(wallet.publicKey(account, 2, external)).thenReturn(publicKey2)
//
//        whenever(addressSelector.getAddressVariants(publicKey0)).thenReturn(addresses0)
//        whenever(addressSelector.getAddressVariants(publicKey1)).thenReturn(addresses1)
//        whenever(addressSelector.getAddressVariants(publicKey2)).thenReturn(addresses2)
//
//        val blockHash = "00ffee0abe"
//        val blockHeight = 123123
//        val transactionResponse = mock(BCoinTransactionResponse::class.java)
//
//        whenever(transactionResponse.blockHash).thenReturn(blockHash)
//        whenever(transactionResponse.blockHeight).thenReturn(blockHeight)
//
//        val transactionResponses = listOf(transactionResponse)
//        whenever(apiManager.getTransactions(addressesForCycle1)).thenReturn(transactionResponses)
//
//        whenever(xxx.lastUsedIndex(listOf(addresses0, addresses1, addresses2), transactionResponses)).thenReturn(1)
//
//        whenever(wallet.publicKey(account, 3, external)).thenReturn(publicKey3)
//        whenever(wallet.publicKey(account, 4, external)).thenReturn(publicKey4)
//
//        whenever(addressSelector.getAddressVariants(publicKey3)).thenReturn(addresses3)
//        whenever(addressSelector.getAddressVariants(publicKey4)).thenReturn(addresses4)
//
//        whenever(apiManager.getTransactions(addressesForCycle2)).thenReturn(listOf())
//
//        syncerApi.fetchFromApi(account, external).test()
//                .assertValue(Pair(listOf(publicKey0, publicKey1, publicKey2, publicKey3, publicKey4), listOf(BlockHash(blockHash, blockHeight))))
//
//
//    }

}