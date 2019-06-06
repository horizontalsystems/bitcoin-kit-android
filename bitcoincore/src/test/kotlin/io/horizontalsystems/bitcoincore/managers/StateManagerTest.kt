package io.horizontalsystems.bitcoincore.managers

import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.network.Network
import junit.framework.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.mockito.Mockito.mock
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

        context("when `restoreFromApi` is true") {
            it("marks as `restored`") {
                stateManager = StateManager(storage, false)
                assertTrue(stateManager.restored)
            }
        }

        context("when already restored") {
            beforeEach {
                whenever(storage.initialRestored).thenReturn(true)
            }

            it("marks as `restored`") {
                stateManager = StateManager(storage, true)
                assertTrue(stateManager.restored)
            }
        }

        context("when not restored yet") {
            beforeEach {
                whenever(storage.initialRestored).thenReturn(false)
            }

            it("marks as not `restored`") {
                stateManager = StateManager(storage, true)
                assertFalse(stateManager.restored)
            }
        }
    }
})
