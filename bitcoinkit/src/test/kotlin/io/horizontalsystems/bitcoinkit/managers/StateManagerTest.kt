package io.horizontalsystems.bitcoinkit.managers

import com.nhaarman.mockito_kotlin.verify
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.network.MainNet
import io.horizontalsystems.bitcoinkit.network.RegTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class StateManagerTest {

    private val storage = mock(IStorage::class.java)

    private lateinit var stateManager: StateManager

    @Test
    fun apiSynced() {
        stateManager = StateManager(storage, MainNet(storage), newWallet = false)
        assertFalse(stateManager.restored)
    }

    @Test
    fun apiSynced_RegTest() {
        stateManager = StateManager(storage, RegTest(storage), newWallet = false)
        assertTrue(stateManager.restored)
    }

    @Test
    fun apiSynced_newWallet() {
        stateManager = StateManager(storage, MainNet(storage), newWallet = true)
        assertTrue(stateManager.restored)
    }

    @Test
    fun apiSynced_SetTrue() {
        stateManager = StateManager(storage, MainNet(storage), newWallet = false)
        stateManager.restored = true

        verify(storage).setInitialRestored(true)
    }

    @Test
    fun apiSynced_SetFalse() {
        stateManager = StateManager(storage, MainNet(storage), newWallet = false)
        stateManager.restored = false

        verify(storage).setInitialRestored(false)
    }
}
