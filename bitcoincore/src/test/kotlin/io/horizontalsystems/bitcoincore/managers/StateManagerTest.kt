package io.horizontalsystems.bitcoincore.managers

// todo: fix it
//class StateManagerTest {
//
//    private val storage = mock(IStorage::class.java)
//
//    private lateinit var stateManager: StateManager
//
//    @Test
//    fun apiSynced() {
//        stateManager = StateManager(storage, MainNet(), newWallet = false)
//        assertFalse(stateManager.restored)
//    }
//
//    @Test
//    fun apiSynced_RegTest() {
//        stateManager = StateManager(storage, RegTest(), newWallet = false)
//        assertTrue(stateManager.restored)
//    }
//
//    @Test
//    fun apiSynced_newWallet() {
//        stateManager = StateManager(storage, MainNet(), newWallet = true)
//        assertTrue(stateManager.restored)
//    }
//
//    @Test
//    fun apiSynced_SetTrue() {
//        stateManager = StateManager(storage, MainNet(), newWallet = false)
//        stateManager.restored = true
//
//        verify(storage).setInitialRestored(true)
//    }
//
//    @Test
//    fun apiSynced_SetFalse() {
//        stateManager = StateManager(storage, MainNet(), newWallet = false)
//        stateManager.restored = false
//
//        verify(storage).setInitialRestored(false)
//    }
//}
