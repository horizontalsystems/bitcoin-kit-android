//package io.horizontalsystems.bitcoincore.managers
//
//import com.nhaarman.mockitokotlin2.mock
//import com.nhaarman.mockitokotlin2.whenever
//import io.horizontalsystems.bitcoincore.core.Wallet
//import io.horizontalsystems.bitcoincore.models.BlockHash
//import io.horizontalsystems.bitcoincore.models.PublicKey
//import org.mockito.Mockito
//import org.spekframework.spek2.Spek
//import org.spekframework.spek2.style.specification.describe
//
//object BlockDiscoveryBatchTest : Spek({
//
//    val wallet = Mockito.mock(Wallet::class.java)
//    val blockHashFetcher = Mockito.mock(BlockHashFetcher::class.java)
//
//    lateinit var syncerApi: BlockDiscoveryBatch
//
//    beforeEachTest {
//        whenever(wallet.gapLimit).thenReturn(3)
//
//        syncerApi = BlockDiscoveryBatch(wallet, blockHashFetcher, 100)
//    }
//
//    describe("#discoverBlockHashes") {
//
//        it("fetches blocks recursively") {
//            val account = 0
//            val external = true
//
//            val publicKey0 = mock<PublicKey>()
//            val publicKey1 = mock<PublicKey>()
//            val publicKey2 = mock<PublicKey>()
//            val publicKey3 = mock<PublicKey>()
//            val publicKey4 = mock<PublicKey>()
//            val publicKeysCycle1 = listOf(publicKey0, publicKey1, publicKey2)
//            val publicKeysCycle2 = listOf(publicKey3, publicKey4)
//            val lastUsedPublicKeyIndex = 1
//
//            whenever(wallet.publicKey(account, 0, external)).thenReturn(publicKey0)
//            whenever(wallet.publicKey(account, 1, external)).thenReturn(publicKey1)
//            whenever(wallet.publicKey(account, 2, external)).thenReturn(publicKey2)
//
//            val blockHash = mock<BlockHash>()
//
//            whenever(blockHashFetcher.getBlockHashes(publicKeysCycle1)).thenReturn(Pair(listOf(blockHash), lastUsedPublicKeyIndex))
//
//            whenever(wallet.publicKey(account, 3, external)).thenReturn(publicKey3)
//            whenever(wallet.publicKey(account, 4, external)).thenReturn(publicKey4)
//
//            whenever(blockHashFetcher.getBlockHashes(publicKeysCycle2)).thenReturn(Pair(listOf(), -1))
//
//            syncerApi.discoverBlockHashes(account, external)
//                    .test()
//                    .assertValue(Pair(publicKeysCycle1 + publicKeysCycle2, listOf(blockHash)))
//
//        }
//    }
//
//})
