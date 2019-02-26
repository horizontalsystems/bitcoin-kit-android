package io.horizontalsystems.bitcoinkit.managers

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.core.Wallet
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.PublicKey
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class BlockDiscoveryBatchTest {
    private val wallet = Mockito.mock(Wallet::class.java)
    private val blockHashFetcher = Mockito.mock(BlockHashFetcherBCoin::class.java)

    private lateinit var syncerApi: BlockDiscoveryBatch

    @Before
    fun setup() {
        whenever(wallet.gapLimit).thenReturn(3)

        syncerApi = BlockDiscoveryBatch(wallet, blockHashFetcher, 100)
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

        whenever(blockHashFetcher.getBlockHashes(publicKeysCycle1)).thenReturn(Pair(listOf(blockHash), lastUsedPublicKeyIndex))

        whenever(wallet.publicKey(account, 3, external)).thenReturn(publicKey3)
        whenever(wallet.publicKey(account, 4, external)).thenReturn(publicKey4)

        whenever(blockHashFetcher.getBlockHashes(publicKeysCycle2)).thenReturn(Pair(listOf(), -1))

        syncerApi.discoverBlockHashes(account, external)
                .test()
                .assertValue(Pair(publicKeysCycle1 + publicKeysCycle2, listOf(blockHash)))

    }


}