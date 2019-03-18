package io.horizontalsystems.bitcoinkit.blocks

import io.horizontalsystems.bitcoinkit.BitcoinKit.KitState
import io.horizontalsystems.bitcoinkit.core.KitStateProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.verifyNew
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(KitStateProvider::class)

class KitStateProviderTest {

    private val kitStateListener = mock(KitStateProvider.Listener::class.java)
    private val kitStateProvider = KitStateProvider()
    private val syncing = mock(KitState.Syncing::class.java)

    @Before
    fun setup() {
        kitStateProvider.listener = kitStateListener

        PowerMockito.whenNew(KitState.Syncing::class.java)
                .withAnyArguments()
                .thenReturn(syncing)
    }

    @Test
    fun onSyncStart() {
        kitStateProvider.onSyncStart()

        verifyNew(KitState.Syncing::class.java).withArguments(0.0)
    }

    @Test
    fun onSyncFinish() {
        kitStateProvider.onSyncFinish()

        verify(kitStateListener).onKitStateUpdate(KitState.Synced)
    }

    @Test
    fun onSyncStop() {
        kitStateProvider.onSyncStart()
        kitStateProvider.onSyncStop()

        verify(kitStateListener).onKitStateUpdate(KitState.NotSynced)
    }

    @Test
    fun onInitialBestBlockHeight() {
        kitStateProvider.onInitialBestBlockHeightUpdate(100)
        verifyNoMoreInteractions(kitStateListener)
    }

    @Test
    fun onCurrentBestBlockHeight() {
        kitStateProvider.onInitialBestBlockHeightUpdate(100)
        kitStateProvider.onCurrentBestBlockHeightUpdate(101, 200)

        verifyNew(KitState.Syncing::class.java).withArguments(0.01)
    }

    @Test
    fun onCurrentBestBlockHeightUpdate_heightLessThanInitialHeight() {
        kitStateProvider.onInitialBestBlockHeightUpdate(100)
        kitStateProvider.onCurrentBestBlockHeightUpdate(99, 200)

        verifyNew(KitState.Syncing::class.java).withArguments(0.0)
    }

    @Test
    fun onCurrentBestBlockHeightUpdate_heightMoreThanMaxHeight() {
        kitStateProvider.onInitialBestBlockHeightUpdate(100)
        kitStateProvider.onCurrentBestBlockHeightUpdate(201, 200)

        verify(kitStateListener).onKitStateUpdate(KitState.Synced)
    }

    @Test
    fun onCurrentBestBlockHeightUpdate_MaxHeightLessThanInitialHeight() {
        kitStateProvider.onInitialBestBlockHeightUpdate(100)
        kitStateProvider.onCurrentBestBlockHeightUpdate(99, 99)

        verify(kitStateListener).onKitStateUpdate(KitState.Synced)
    }
}
