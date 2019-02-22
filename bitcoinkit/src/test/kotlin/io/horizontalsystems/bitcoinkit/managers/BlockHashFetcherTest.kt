package io.horizontalsystems.bitcoinkit.managers

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.models.PublicKey
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class BlockHashFetcherTest {
    private val apiManager = Mockito.mock(ApiManager::class.java)
    private val addressSelector = Mockito.mock(IAddressSelector::class.java)
    private val yyy = Mockito.mock(BlockHashFetcherHelper::class.java)
    private val blockHashFetcher = BlockHashFetcher(addressSelector, apiManager, yyy)


    @Test
    fun getEmptyBlockHashes() {
        val publicKey0 = mock<PublicKey>()
        val publicKey1 = mock<PublicKey>()
        val publicKey2 = mock<PublicKey>()

        val addresses0 = listOf("0_0", "0_1")
        val addresses1 = listOf("1_0", "1_1")
        val addresses2 = listOf("2_0", "2_1")

        val addresses = addresses0 + addresses1 + addresses2

        whenever(addressSelector.getAddressVariants(publicKey0)).thenReturn(addresses0)
        whenever(addressSelector.getAddressVariants(publicKey1)).thenReturn(addresses1)
        whenever(addressSelector.getAddressVariants(publicKey2)).thenReturn(addresses2)

        whenever(apiManager.getTransactions(addresses)).thenReturn(listOf())

        val (blockHashes, lastUsedIndex) = blockHashFetcher.getBlockHashes(listOf(publicKey0, publicKey1, publicKey2))

        Assert.assertTrue(blockHashes.isEmpty())
        Assert.assertEquals(-1, lastUsedIndex)
    }

    @Test
    fun getNonEmptyBlockHashes() {
        val publicKey0 = mock<PublicKey>()
        val publicKey1 = mock<PublicKey>()
        val publicKey2 = mock<PublicKey>()

        val addresses0 = listOf("0_0", "0_1")
        val addresses1 = listOf("1_0", "1_1")
        val addresses2 = listOf("2_0", "2_1")

        val addresses = addresses0 + addresses1 + addresses2

        whenever(addressSelector.getAddressVariants(publicKey0)).thenReturn(addresses0)
        whenever(addressSelector.getAddressVariants(publicKey1)).thenReturn(addresses1)
        whenever(addressSelector.getAddressVariants(publicKey2)).thenReturn(addresses2)

        val transactionResponse0 = mock<BCoinTransactionResponse>()
        whenever(transactionResponse0.blockHeight).thenReturn(1234)
        whenever(transactionResponse0.blockHash).thenReturn("1234")
        whenever(transactionResponse0.txOutputs).thenReturn(listOf())

        val transactionResponse1 = mock<BCoinTransactionResponse>()
        whenever(transactionResponse1.blockHeight).thenReturn(5678)
        whenever(transactionResponse1.blockHash).thenReturn("5678")
        whenever(transactionResponse1.txOutputs).thenReturn(listOf())

        whenever(apiManager.getTransactions(addresses)).thenReturn(listOf(transactionResponse0, transactionResponse1))
        val lastUsedIndex = 1
        whenever(yyy.lastUsedIndex(listOf(addresses0, addresses1, addresses2), listOf())).thenReturn(lastUsedIndex)

        val (blockHashes, actualLastUsedIndex) = blockHashFetcher.getBlockHashes(listOf(publicKey0, publicKey1, publicKey2))

        Assert.assertEquals(lastUsedIndex, actualLastUsedIndex)
        Assert.assertEquals(2, blockHashes.size)
        Assert.assertEquals("1234", blockHashes.first().reversedHeaderHashHex)
        Assert.assertEquals(1234, blockHashes.first().height)
        Assert.assertEquals("5678", blockHashes.last().reversedHeaderHashHex)
        Assert.assertEquals(5678, blockHashes.last().height)
    }
}