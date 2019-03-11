package io.horizontalsystems.bitcoinkit.managers

import com.nhaarman.mockito_kotlin.*
import helpers.RxTestRule
import io.horizontalsystems.bitcoinkit.RealmFactoryMock
import io.horizontalsystems.bitcoinkit.core.ISyncStateListener
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.network.peer.PeerGroup
import io.reactivex.Single
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.util.concurrent.TimeUnit

class InitialSyncerTest {

//    private val factories = RealmFactoryMock()
//    private val initialSyncerApi = mock(BlockDiscoveryBatch::class.java)
//    private val kitStateListener = mock(ISyncStateListener::class.java)
//
//    private val stateManager = mock(StateManager::class.java)
//    private val peerGroup = mock(PeerGroup::class.java)
//    private val addressManager = mock(AddressManager::class.java)
//    private val realm = factories.realmFactory.realm
//    private val apiRespStub = Single.just(Pair(listOf(PublicKey()), listOf(BlockHash())))
//
//    private lateinit var initialSyncer: InitialSyncer
//
//    @Before
//    fun setup() {
//        RxTestRule.setup()
//
//        initialSyncer = InitialSyncer(factories.realmFactory, initialSyncerApi, stateManager, addressManager, peerGroup, kitStateListener)
//    }
//
//    @After
//    fun tearDown() {
//        realm.executeTransaction {
//            it.deleteAll()
//        }
//    }
//
//    @Test
//    fun sync() {
//        whenever(stateManager.restored).thenReturn(false)
//        whenever(initialSyncerApi.discoverBlockHashes(0, true)).thenReturn(apiRespStub)
//        whenever(initialSyncerApi.discoverBlockHashes(0, false)).thenReturn(apiRespStub)
//        whenever(initialSyncerApi.discoverBlockHashes(1, true)).thenReturn(Single.just(Pair(listOf(), listOf())))
//        whenever(initialSyncerApi.discoverBlockHashes(1, false)).thenReturn(Single.just(Pair(listOf(), listOf())))
//
//        initialSyncer.sync()
//
//        verify(peerGroup).start()
//        verify(kitStateListener).onSyncStart()
//        verify(addressManager, times(2)).addKeys(any())
//    }
//
//    @Test
//    fun stop() {
//        val responseWithTimeout = apiRespStub.timeout(1, TimeUnit.SECONDS)
//
//        whenever(stateManager.restored).thenReturn(false)
//        whenever(initialSyncerApi.discoverBlockHashes(0, true)).thenReturn(responseWithTimeout)
//        whenever(initialSyncerApi.discoverBlockHashes(0, false)).thenReturn(responseWithTimeout)
//
//        initialSyncer.sync()
//        initialSyncer.stop()
//
//        verify(peerGroup).close()
//    }
//
//    // @Test
//    // fun refresh() {
//    //     whenever(stateManager.restored).thenReturn(false)
//    //     whenever(initialSyncerApi.discoverBlockHashes(true)).thenReturn(apiRespStub)
//    //     whenever(initialSyncerApi.discoverBlockHashes(false)).thenReturn(apiRespStub)
//    //
//    //     initialSyncer.sync()
//    //     initialSyncer.sync() // refresh
//    //
//    //     verify(peerGroup).start()
//    //     verify(kitStateListener).onSyncStart()
//    //     verify(addressManager).addKeys(any())
//    // }
//
//    @Test
//    fun sync_apiSynced() {
//        whenever(stateManager.restored).thenReturn(true)
//
//        initialSyncer.sync()
//
//        verify(peerGroup).start()
//    }
//
//    @Test
//    fun sync_apiNotSynced_blocksDiscoveredSuccess() {
//        val externalPublicKey1 = PublicKey().apply {
//            external = true
//            index = 555
//            publicKeyHex = "e555"
//        }
//        val internalPublicKey1 = PublicKey().apply {
//            external = false
//            index = 123
//            publicKeyHex = "e123"
//        }
//
//        var blockIndex = 0
//
//        val blockExternal1 = BlockHash().apply {
//            reversedHeaderHashHex = blockIndex++.toString()
//        }
//        val blockExternal2 = BlockHash().apply {
//            reversedHeaderHashHex = blockIndex++.toString()
//        }
//        val blockInternal1 = BlockHash().apply {
//            reversedHeaderHashHex = blockIndex++.toString()
//        }
//        val blockInternal2 = BlockHash().apply {
//            reversedHeaderHashHex = blockIndex++.toString()
//        }
//
//        val externalObservable = Single.just(Pair(listOf(externalPublicKey1), listOf(blockExternal1, blockExternal2)))
//        val internalObservable = Single.just(Pair(listOf(internalPublicKey1), listOf(blockInternal1, blockInternal2)))
//
//        whenever(stateManager.restored).thenReturn(false)
//
//        whenever(initialSyncerApi.discoverBlockHashes(0, true)).thenReturn(externalObservable)
//        whenever(initialSyncerApi.discoverBlockHashes(0, false)).thenReturn(internalObservable)
//        whenever(initialSyncerApi.discoverBlockHashes(1, true)).thenReturn(Single.just(Pair(listOf(), listOf())))
//        whenever(initialSyncerApi.discoverBlockHashes(1, false)).thenReturn(Single.just(Pair(listOf(), listOf())))
//
//        initialSyncer.sync()
//
//        verify(stateManager).restored = true
//        verify(peerGroup).start()
//        inOrder(addressManager).let {
//            it.verify(addressManager).addKeys(listOf(externalPublicKey1, internalPublicKey1))
//            it.verify(addressManager).addKeys(listOf())
//        }
//
//        val actualBlocks = realm.where(BlockHash::class.java).findAll()
//
//        assertTrue(containsBlock(actualBlocks, blockExternal1))
//        assertTrue(containsBlock(actualBlocks, blockExternal2))
//        assertTrue(containsBlock(actualBlocks, blockInternal1))
//        assertTrue(containsBlock(actualBlocks, blockInternal2))
//    }
//
//    @Test
//    fun sync_apiNotSynced_blocksDiscoveredFail() {
//        whenever(stateManager.restored).thenReturn(false)
//
//        whenever(initialSyncerApi.discoverBlockHashes(0, true)).thenReturn(Single.error(Exception()))
//        whenever(initialSyncerApi.discoverBlockHashes(0, false)).thenReturn(Single.error(Exception()))
//
//        initialSyncer.sync()
//
//        verify(stateManager, never()).restored = true
//        verifyNoMoreInteractions(peerGroup)
//
//        assertTrue(realm.where(PublicKey::class.java).findAll().isEmpty())
//        assertTrue(realm.where(BlockHash::class.java).findAll().isEmpty())
//    }
//
//    private fun containsBlock(blocks: List<BlockHash>, block: BlockHash) =
//            blocks.any { it.reversedHeaderHashHex == block.reversedHeaderHashHex }

}
