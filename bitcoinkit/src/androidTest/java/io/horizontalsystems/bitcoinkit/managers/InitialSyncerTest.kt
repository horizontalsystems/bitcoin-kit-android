package io.horizontalsystems.bitcoinkit.managers

import com.nhaarman.mockito_kotlin.check
import com.nhaarman.mockito_kotlin.whenever
import helpers.RxTestRule
import io.horizontalsystems.bitcoinkit.RealmFactoryMock
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.network.peer.PeerGroup
import io.reactivex.Single
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class InitialSyncerTest {

    private val factories = RealmFactoryMock()
    private val initialSyncerApi = mock(InitialSyncerApi::class.java)

    private val stateManager = mock(StateManager::class.java)
    private val peerGroup = mock(PeerGroup::class.java)
    private val addressManager = mock(AddressManager::class.java)
    private val realm = factories.realmFactory.realm

    private lateinit var initialSyncer: InitialSyncer

    @Before
    fun setup() {
        RxTestRule.setup()

        initialSyncer = InitialSyncer(factories.realmFactory, initialSyncerApi, stateManager, addressManager, peerGroup)
    }

    @After
    fun tearDown() {
        realm.executeTransaction {
            it.deleteAll()
        }
    }

    @Test
    fun sync_apiSynced() {
        whenever(stateManager.apiSynced).thenReturn(true)

        initialSyncer.sync()

        verify(peerGroup).start()
    }

    @Test
    fun sync_apiNotSynced_blocksDiscoveredSuccess() {
        val externalPublicKey1 = PublicKey().apply {
            external = true
            index = 555
            publicKeyHex = "e555"
        }
        val internalPublicKey1 = PublicKey().apply {
            external = false
            index = 123
            publicKeyHex = "e123"
        }

        var blockIndex = 0

        val blockExternal1 = BlockHash().apply {
            reversedHeaderHashHex = blockIndex++.toString()
        }
        val blockExternal2 = BlockHash().apply {
            reversedHeaderHashHex = blockIndex++.toString()
        }
        val blockInternal1 = BlockHash().apply {
            reversedHeaderHashHex = blockIndex++.toString()
        }
        val blockInternal2 = BlockHash().apply {
            reversedHeaderHashHex = blockIndex++.toString()
        }

        val externalObservable = Single.just(Pair(listOf(externalPublicKey1), listOf(blockExternal1, blockExternal2)))
        val internalObservable = Single.just(Pair(listOf(internalPublicKey1), listOf(blockInternal1, blockInternal2)))

        whenever(stateManager.apiSynced).thenReturn(false)

        whenever(initialSyncerApi.fetchFromApi(true)).thenReturn(externalObservable)
        whenever(initialSyncerApi.fetchFromApi(false)).thenReturn(internalObservable)

        initialSyncer.sync()

        verify(stateManager).apiSynced = true
        verify(peerGroup).start()
        verify(addressManager).addKeys(check { actualPublicKeys ->
            Assert.assertTrue(containsKey(actualPublicKeys, externalPublicKey1))
            Assert.assertTrue(containsKey(actualPublicKeys, internalPublicKey1))
        })

        val actualBlocks = realm.where(BlockHash::class.java).findAll()

        Assert.assertTrue(containsBlock(actualBlocks, blockExternal1))
        Assert.assertTrue(containsBlock(actualBlocks, blockExternal2))
        Assert.assertTrue(containsBlock(actualBlocks, blockInternal1))
        Assert.assertTrue(containsBlock(actualBlocks, blockInternal2))
    }

    @Test
    fun sync_apiNotSynced_blocksDiscoveredFail() {
        whenever(stateManager.apiSynced).thenReturn(false)

        whenever(initialSyncerApi.fetchFromApi(true)).thenReturn(Single.error(Exception()))
        whenever(initialSyncerApi.fetchFromApi(false)).thenReturn(Single.error(Exception()))

        initialSyncer.sync()

        verify(stateManager, never()).apiSynced = true
        verifyNoMoreInteractions(peerGroup)

        Assert.assertTrue(realm.where(PublicKey::class.java).findAll().isEmpty())
        Assert.assertTrue(realm.where(Block::class.java).findAll().isEmpty())
    }

    private fun containsKey(keys: List<PublicKey>, key: PublicKey) =
            keys.any { it.external == key.external && it.index == key.index }

    private fun containsBlock(blocks: List<BlockHash>, block: BlockHash) =
            blocks.any { it.reversedHeaderHashHex == block.reversedHeaderHashHex }

}
