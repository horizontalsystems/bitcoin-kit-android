package io.horizontalsystems.bitcoincore.managers

import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.network.Network
import junit.framework.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class StateManagerTest {

    private val storage = mock(IStorage::class.java)

    private lateinit var stateManager: StateManager
    private val networkSyncableFromApi = mock(Network::class.java)
    private val networkNotSyncableFromApi = mock(Network::class.java)

    @Before
    fun setup() {
        whenever(networkSyncableFromApi.syncableFromApi).thenReturn(true)
        whenever(networkNotSyncableFromApi.syncableFromApi).thenReturn(false)
    }

    @Test
    fun apiSynced() {

        stateManager = StateManager(storage, networkSyncableFromApi, newWallet = false)
        assertFalse(stateManager.restored)
    }

    @Test
    fun apiSynced_RegTest() {
        stateManager = StateManager(storage, networkNotSyncableFromApi, newWallet = false)
        assertTrue(stateManager.restored)
    }

    @Test
    fun apiSynced_newWallet() {
        stateManager = StateManager(storage, networkSyncableFromApi, newWallet = true)
        assertTrue(stateManager.restored)
    }

    @Test
    fun apiSynced_SetTrue() {
        stateManager = StateManager(storage, networkSyncableFromApi, newWallet = false)
        stateManager.restored = true

        verify(storage).setInitialRestored(true)
    }

    @Test
    fun apiSynced_SetFalse() {
        stateManager = StateManager(storage, networkSyncableFromApi, newWallet = false)
        stateManager.restored = false

        verify(storage).setInitialRestored(false)
    }
}
