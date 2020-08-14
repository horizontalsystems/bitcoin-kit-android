//package io.horizontalsystems.bitcoincore.managers
//
//import com.nhaarman.mockitokotlin2.mock
//import com.nhaarman.mockitokotlin2.whenever
//import io.horizontalsystems.bitcoincore.core.IInitialSyncApi
//import io.horizontalsystems.bitcoincore.extensions.toReversedHex
//import io.horizontalsystems.bitcoincore.models.PublicKey
//import org.junit.Assert
//import org.mockito.Mockito
//import org.spekframework.spek2.Spek
//import org.spekframework.spek2.style.specification.describe
//
//object BlockHashFetcherTest : Spek({
//
//    val restoreKeyConverter = Mockito.mock(IRestoreKeyConverter::class.java)
//    val initialSyncApi = Mockito.mock(IInitialSyncApi::class.java)
//    val helper = Mockito.mock(BlockHashFetcherHelper::class.java)
//
//    val blockHashFetcher = BlockHashFetcher(restoreKeyConverter, initialSyncApi, helper)
//
//    describe("#getBlockHashes") {
//
//        it("gets empty BlockHash's") {
//            val publicKey0 = mock<PublicKey>()
//            val publicKey1 = mock<PublicKey>()
//            val publicKey2 = mock<PublicKey>()
//
//            val addresses0 = listOf("0_0", "0_1")
//            val addresses1 = listOf("1_0", "1_1")
//            val addresses2 = listOf("2_0", "2_1")
//
//            val addresses = addresses0 + addresses1 + addresses2
//
//            whenever(restoreKeyConverter.keysForApiRestore(publicKey0)).thenReturn(addresses0)
//            whenever(restoreKeyConverter.keysForApiRestore(publicKey1)).thenReturn(addresses1)
//            whenever(restoreKeyConverter.keysForApiRestore(publicKey2)).thenReturn(addresses2)
//
//            whenever(initialSyncApi.getTransactions(addresses)).thenReturn(listOf())
//
//            val (blockHashes, lastUsedIndex) = blockHashFetcher.getBlockHashes(listOf(publicKey0, publicKey1, publicKey2))
//
//            Assert.assertTrue(blockHashes.isEmpty())
//            Assert.assertEquals(-1, lastUsedIndex)
//        }
//
//        it("gets non empty BlockHash's") {
//            val publicKey0 = mock<PublicKey>()
//            val publicKey1 = mock<PublicKey>()
//            val publicKey2 = mock<PublicKey>()
//
//            val addresses0 = listOf("0_0", "0_1")
//            val addresses1 = listOf("1_0", "1_1")
//            val addresses2 = listOf("2_0", "2_1")
//
//            val addresses = addresses0 + addresses1 + addresses2
//
//            whenever(restoreKeyConverter.keysForApiRestore(publicKey0)).thenReturn(addresses0)
//            whenever(restoreKeyConverter.keysForApiRestore(publicKey1)).thenReturn(addresses1)
//            whenever(restoreKeyConverter.keysForApiRestore(publicKey2)).thenReturn(addresses2)
//
//            val transactionResponse0 = mock<TransactionItem>()
//            whenever(transactionResponse0.blockHeight).thenReturn(1234)
//            whenever(transactionResponse0.blockHash).thenReturn("1234")
//            whenever(transactionResponse0.txOutputs).thenReturn(listOf())
//
//            val transactionResponse1 = mock<TransactionItem>()
//            whenever(transactionResponse1.blockHeight).thenReturn(5678)
//            whenever(transactionResponse1.blockHash).thenReturn("5678")
//            whenever(transactionResponse1.txOutputs).thenReturn(listOf())
//
//            whenever(initialSyncApi.getTransactions(addresses)).thenReturn(listOf(transactionResponse0, transactionResponse1))
//            val lastUsedIndex = 1
//            whenever(helper.lastUsedIndex(listOf(addresses0, addresses1, addresses2), listOf())).thenReturn(lastUsedIndex)
//
//            val (blockHashes, actualLastUsedIndex) = blockHashFetcher.getBlockHashes(listOf(publicKey0, publicKey1, publicKey2))
//
//            Assert.assertEquals(lastUsedIndex, actualLastUsedIndex)
//            Assert.assertEquals(2, blockHashes.size)
//            Assert.assertEquals("1234", blockHashes.first().headerHash.toReversedHex())
//            Assert.assertEquals(1234, blockHashes.first().height)
//            Assert.assertEquals("5678", blockHashes.last().headerHash.toReversedHex())
//            Assert.assertEquals(5678, blockHashes.last().height)
//        }
//    }
//})
