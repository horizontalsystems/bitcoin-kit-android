package io.horizontalsystems.bitcoinkit.blocks

import io.horizontalsystems.bitcoinkit.RealmFactoryMock
import io.horizontalsystems.bitcoinkit.core.KitStateProvider
import io.horizontalsystems.bitcoinkit.managers.AddressManager
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.network.MainNet
import io.horizontalsystems.bitcoinkit.transactions.TransactionProcessor
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class BlockSyncerTest_localKnownBestBlockHeight {
    private val factories = RealmFactoryMock()
    private val realm = factories.realmFactory.realm
    private val network = MainNet()
    private val blockchain = mock(Blockchain::class.java)
    private val transactionProcessor = mock(TransactionProcessor::class.java)
    private val addressManager = mock(AddressManager::class.java)
    private val bloomFilterManager = mock(BloomFilterManager::class.java)
    private val processSyncer = mock(KitStateProvider::class.java)

    private lateinit var blockSyncer: BlockSyncer

    @Before
    fun setup() {
        realm.executeTransaction {
            realm.deleteAll()
        }

        blockSyncer = BlockSyncer(factories.realmFactory, blockchain, transactionProcessor, addressManager, bloomFilterManager, processSyncer, network)

    }

    @Test
    fun localKnownBestBlockHeight_noBlockHashes() {
        val expectedKnownBestBlockHeight = network.checkpointBlock.height

        Assert.assertEquals(expectedKnownBestBlockHeight, blockSyncer.localKnownBestBlockHeight)
    }

    @Test
    fun localKnownBestBlockHeight_hasBlockHash() {
        val expectedKnownBestBlockHeight = network.checkpointBlock.height + 2

        realm.executeTransaction {
            realm.insert(BlockHash(byteArrayOf(1), 0, 1))
            realm.insert(BlockHash(byteArrayOf(2), 0, 2))
        }

        Assert.assertEquals(expectedKnownBestBlockHeight, blockSyncer.localKnownBestBlockHeight)
    }

    @Test
    fun localKnownBestBlockHeight_hasBlockHashAlreadyDownloaded() {
        val expectedKnownBestBlockHeight = network.checkpointBlock.height + 2

        realm.executeTransaction {
            realm.insert(BlockHash(network.checkpointBlock.headerHash, 0, 1))
            realm.insert(BlockHash(byteArrayOf(1), 0, 2))
            realm.insert(BlockHash(byteArrayOf(2), 0, 3))
        }

        Assert.assertEquals(expectedKnownBestBlockHeight, blockSyncer.localKnownBestBlockHeight)
    }

    @Test
    fun localKnownBestBlockHeight_hasBlockHashRestored() {
        val expectedKnownBestBlockHeight = network.checkpointBlock.height + 1

        realm.executeTransaction {
            realm.insert(BlockHash(byteArrayOf(1), 12, 0))
            realm.insert(BlockHash(byteArrayOf(2), 0, 5))
        }

        Assert.assertEquals(expectedKnownBestBlockHeight, blockSyncer.localKnownBestBlockHeight)
    }
}
