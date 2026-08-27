package io.horizontalsystems.bitcoincore.apisync

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.mockito.Mockito.verifyNoInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.apisync.blockchair.BlockchairBlockHashFetcher
import org.junit.Assert.assertEquals
import org.junit.Test

class BlockHashFetcherTest {

    private val hsFetcher = mock<HsBlockHashFetcher>()
    private val blockchairFetcher = mock<BlockchairBlockHashFetcher>()
    private val fetcher = BlockHashFetcher(hsFetcher, blockchairFetcher, checkpointHeight = 1000)

    @Test
    fun splitsHeightsAroundCheckpoint() {
        whenever(hsFetcher.fetch(listOf(900, 1000))).thenReturn(mapOf(900 to "a", 1000 to "b"))
        whenever(blockchairFetcher.fetch(listOf(1001))).thenReturn(mapOf(1001 to "c"))

        assertEquals(mapOf(900 to "a", 1000 to "b", 1001 to "c"), fetcher.fetch(listOf(900, 1000, 1001)))
    }

    @Test
    fun fallsBackToBlockchairForHeightsMissingFromHs() {
        whenever(hsFetcher.fetch(listOf(900, 950))).thenReturn(mapOf(900 to "a"))
        whenever(blockchairFetcher.fetch(listOf(950))).thenReturn(mapOf(950 to "b"))

        assertEquals(mapOf(900 to "a", 950 to "b"), fetcher.fetch(listOf(900, 950)))
    }

    @Test
    fun doesNotCallBlockchairWhenHsResolvesEverything() {
        whenever(hsFetcher.fetch(listOf(900))).thenReturn(mapOf(900 to "a"))

        assertEquals(mapOf(900 to "a"), fetcher.fetch(listOf(900)))
        verifyNoInteractions(blockchairFetcher)
    }

    @Test
    fun skipsHsForHeightsAboveCheckpoint() {
        whenever(blockchairFetcher.fetch(listOf(1001))).thenReturn(mapOf(1001 to "c"))

        fetcher.fetch(listOf(1001))
        verifyNoInteractions(hsFetcher)
        verify(blockchairFetcher).fetch(listOf(1001))
    }
}
