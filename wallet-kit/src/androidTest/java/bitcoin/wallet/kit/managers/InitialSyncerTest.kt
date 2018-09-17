package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.Factories
import bitcoin.wallet.kit.RxBaseTest
import bitcoin.wallet.kit.TestUtils.whenever
import bitcoin.wallet.kit.hdwallet.PublicKey
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.network.PeerGroup
import io.reactivex.Observable
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class InitialSyncerTest {

    private val factories = Factories()
    private val blockDiscover = mock(BlockDiscover::class.java)

    private val stateManager = mock(StateManager::class.java)
    private val peerGroup = mock(PeerGroup::class.java)
    private val realm = factories.realmFactory.realm

    private lateinit var initialSyncer: InitialSyncer

    @Before
    fun setup() {
        RxBaseTest.setup()

        initialSyncer = InitialSyncer(factories.realmFactory, blockDiscover, stateManager, peerGroup)
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
            address = "e555"
        }
        val internalPublicKey1 = PublicKey().apply {
            external = false
            index = 123
            address = "e123"
        }

        var blockIndex = 0

        val blockExternal1 = Block().apply {
            reversedHeaderHashHex = blockIndex++.toString()
        }
        val blockExternal2 = Block().apply {
            reversedHeaderHashHex = blockIndex++.toString()
        }
        val blockInternal1 = Block().apply {
            reversedHeaderHashHex = blockIndex++.toString()
        }
        val blockInternal2 = Block().apply {
            reversedHeaderHashHex = blockIndex++.toString()
        }

        val externalObservable = Observable.just(Pair(listOf(externalPublicKey1), listOf(blockExternal1, blockExternal2)))
        val internalObservable = Observable.just(Pair(listOf(internalPublicKey1), listOf(blockInternal1, blockInternal2)))

        whenever(stateManager.apiSynced).thenReturn(false)

        whenever(blockDiscover.fetchFromApi(true)).thenReturn(externalObservable)
        whenever(blockDiscover.fetchFromApi(false)).thenReturn(internalObservable)

        initialSyncer.sync()

        verify(stateManager).apiSynced = true
        verify(peerGroup).start()

        val actualPublicKeys = realm.where(PublicKey::class.java).findAll()

        Assert.assertTrue(containsKey(actualPublicKeys, externalPublicKey1))
        Assert.assertTrue(containsKey(actualPublicKeys, internalPublicKey1))

        val actualBlocks = realm.where(Block::class.java).findAll()

        Assert.assertTrue(containsBlock(actualBlocks, blockExternal1))
        Assert.assertTrue(containsBlock(actualBlocks, blockExternal2))
        Assert.assertTrue(containsBlock(actualBlocks, blockInternal1))
        Assert.assertTrue(containsBlock(actualBlocks, blockInternal2))
    }

    @Test
    fun sync_apiNotSynced_blocksDiscoveredFail() {
        whenever(stateManager.apiSynced).thenReturn(false)

        whenever(blockDiscover.fetchFromApi(true)).thenReturn(Observable.error(Exception()))
        whenever(blockDiscover.fetchFromApi(false)).thenReturn(Observable.error(Exception()))

        initialSyncer.sync()

        verify(stateManager, never()).apiSynced = true
        verifyNoMoreInteractions(peerGroup)

        Assert.assertTrue(realm.where(PublicKey::class.java).findAll().isEmpty())
        Assert.assertTrue(realm.where(Block::class.java).findAll().isEmpty())
    }

    private fun containsKey(keys: List<PublicKey>, key: PublicKey) =
            keys.any { it.external == key.external && it.index == key.index }

    private fun containsBlock(blocks: List<Block>, block: Block) =
            blocks.any { it.reversedHeaderHashHex == block.reversedHeaderHashHex }

}
