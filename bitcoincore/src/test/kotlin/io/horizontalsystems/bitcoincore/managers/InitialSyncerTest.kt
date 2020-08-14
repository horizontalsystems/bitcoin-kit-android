//package io.horizontalsystems.bitcoincore.managers
//
//import com.nhaarman.mockitokotlin2.*
//import io.horizontalsystems.bitcoincore.RxTestRule
//import io.horizontalsystems.bitcoincore.core.ErrorStorage
//import io.horizontalsystems.bitcoincore.core.IStorage
//import io.horizontalsystems.bitcoincore.core.ISyncStateListener
//import io.horizontalsystems.bitcoincore.models.BlockHash
//import io.horizontalsystems.bitcoincore.models.PublicKey
//import io.reactivex.Single
//import org.mockito.Mockito.mock
//import org.mockito.Mockito.reset
//import org.spekframework.spek2.Spek
//import org.spekframework.spek2.style.specification.describe
//
//object InitialSyncerTest : Spek({
//
//    lateinit var initialSyncer: InitialSyncer
//
//    val storage = mock(IStorage::class.java)
//    val blockDiscovery = mock(IBlockDiscovery::class.java)
//    val stateManager = mock(StateManager::class.java)
//    val publicKeyManager = mock(PublicKeyManager::class.java)
//    val stateListener = mock(ISyncStateListener::class.java)
//    val listener = mock(SyncManager::class.java)
//    val errorStorage = mock(ErrorStorage::class.java)
//
//    beforeEachTest {
//        RxTestRule.setup()
//
//        initialSyncer = InitialSyncer(storage, blockDiscovery, stateManager, publicKeyManager, stateListener, errorStorage)
//        initialSyncer.listener = listener
//    }
//
//    afterEachTest {
//        reset(storage, blockDiscovery, stateManager, publicKeyManager, stateListener, listener)
//    }
//
//    describe("#sync") {
//
//        context("when already synced") {
//            beforeEach {
//                whenever(stateManager.restored).thenReturn(true)
//
//                initialSyncer.sync()
//            }
//
//            it("triggers #onSyncingFinished event on :listener") {
//                verify(listener).onSyncingFinished()
//
//                verifyNoMoreInteractions(listener)
//            }
//
//            it("does not triggers #onSyncStart event on :stateListener") {
//                verify(stateListener, never()).onSyncStart()
//            }
//        }
//
//        context("when not synced yet") {
//            val publicKey1 = mock(PublicKey::class.java)
//            val publicKey2 = mock(PublicKey::class.java)
//            val blockHash1 = mock(BlockHash::class.java)
//            val blockHash2 = mock(BlockHash::class.java)
//
//            beforeEach {
//                whenever(stateManager.restored).thenReturn(false)
//            }
//
//            context("when blockDiscovery fails to fetch block hashes") {
//                beforeEach {
//                    whenever(blockDiscovery.discoverBlockHashes(0, true)).thenReturn(null)
//                    whenever(blockDiscovery.discoverBlockHashes(0, false)).thenReturn(null)
//
//                    initialSyncer.sync()
//                }
//
//                it("triggers #onSyncStop event on :stateListener") {
//                    verify(stateListener).onSyncStart()
//                    verify(stateListener).onSyncStop()
//                }
//
//                it("discovers block hashes only for account 0") {
//                    verify(blockDiscovery).discoverBlockHashes(0, true)
//                    verify(blockDiscovery).discoverBlockHashes(0, false)
//
//                    verify(blockDiscovery, never()).discoverBlockHashes(1, true)
//                    verify(blockDiscovery, never()).discoverBlockHashes(1, false)
//                }
//
//                it("adds error to ErrorStorage") {
//                    verify(errorStorage, atLeast(1)).addApiError(any())
//                }
//            }
//
//            context("when blockDiscovery succeeds") {
//                beforeEach {
//                    // account 1
//                    whenever(blockDiscovery.discoverBlockHashes(0, true)).thenReturn(Single.just(Pair(listOf(publicKey1), listOf(blockHash1))))
//                    whenever(blockDiscovery.discoverBlockHashes(0, false)).thenReturn(Single.just(Pair(listOf(publicKey2), listOf(blockHash2))))
//
//                    // account 2
//                    whenever(blockDiscovery.discoverBlockHashes(1, true)).thenReturn(Single.just(Pair(listOf(), listOf())))
//                    whenever(blockDiscovery.discoverBlockHashes(1, false)).thenReturn(Single.just(Pair(listOf(), listOf())))
//
//                    initialSyncer.sync()
//                }
//
//                it("fetches block hashes for account 0 and 1") {
//                    verify(blockDiscovery).discoverBlockHashes(0, true)
//                    verify(blockDiscovery).discoverBlockHashes(0, false)
//
//                    verify(blockDiscovery).discoverBlockHashes(1, true)
//                    verify(blockDiscovery).discoverBlockHashes(1, false)
//                }
//
//                it("triggers #onSyncStart event on :stateListener") {
//                    verify(stateListener).onSyncStart()
//                    verifyNoMoreInteractions(stateListener)
//                }
//
//                it("adds received addresses to address manager") {
//                    verify(publicKeyManager).addKeys(listOf(publicKey1, publicKey2))
//                    verify(publicKeyManager).addKeys(listOf())
//                }
//
//                it("saves fetched block hashes to storage") {
//                    verify(storage).addBlockHashes(listOf(blockHash1, blockHash2))
//                }
//
//                it("triggers #onSyncingFinished event on :listener") {
//                    verify(stateManager).restored = true
//                    verify(listener).onSyncingFinished()
//                }
//            }
//        }
//    }
//
//    describe("#stop") {
//        it("clears disposables") {}
//    }
//})
