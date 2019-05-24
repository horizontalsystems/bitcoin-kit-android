package io.horizontalsystems.bitcoincore.managers

import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.network.Network
import junit.framework.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object StateManagerTest : Spek({

    lateinit var stateManager: StateManager

    val storage = mock(IStorage::class.java)
    val networkSyncableFromApi = mock(Network::class.java)
    val networkNotSyncableFromApi = mock(Network::class.java)

    beforeEachTest {
        whenever(networkSyncableFromApi.syncableFromApi).thenReturn(true)
        whenever(networkNotSyncableFromApi.syncableFromApi).thenReturn(false)
    }

    describe("#restored") {

        it("apiSynced") {
            stateManager = StateManager(storage, networkSyncableFromApi, newWallet = false)
            assertFalse(stateManager.restored)
        }

        it("apiSynced_RegTest") {
            stateManager = StateManager(storage, networkNotSyncableFromApi, newWallet = false)
            assertTrue(stateManager.restored)
        }

        it("apiSynced_newWallet") {
            stateManager = StateManager(storage, networkSyncableFromApi, newWallet = true)
            assertTrue(stateManager.restored)
        }

        it("apiSynced_SetTrue") {
            stateManager = StateManager(storage, networkSyncableFromApi, newWallet = false)
            stateManager.restored = true

            verify(storage).setInitialRestored(true)
        }

        it("apiSynced_SetFalse") {
            stateManager = StateManager(storage, networkSyncableFromApi, newWallet = false)
            stateManager.restored = false

            verify(storage).setInitialRestored(false)
        }
    }
})
