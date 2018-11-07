package io.horizontalsystems.bitcoinkit.blocks

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.junit.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock

class ProgressSyncerTest {

    private val progressListener = mock(ProgressSyncer.Listener::class.java)
    private val progressSyncer = ProgressSyncer(progressListener)

    @Test
    fun progress_onInitialBestBlockHeight() {
        progressSyncer.onInitialBestBlockHeight(100)

        verifyNoMoreInteractions(progressListener)
    }

    @Test
    fun progress_onReceiveMaxBlockHeight_remoteLonger() {
        progressSyncer.onInitialBestBlockHeight(100)
        progressSyncer.onReceiveMaxBlockHeight(123)

        verify(progressListener).onProgressUpdate(0.0)
        verifyNoMoreInteractions(progressListener)
    }

    @Test
    fun progress_onReceiveMaxBlockHeight_localLonger() {
        progressSyncer.onInitialBestBlockHeight(100)
        progressSyncer.onReceiveMaxBlockHeight(90)

        verify(progressListener).onProgressUpdate(1.0)
        verifyNoMoreInteractions(progressListener)
    }

    @Test
    fun progress_onReceiveMaxBlockHeight_alreadySynced() {
        progressSyncer.onInitialBestBlockHeight(100)
        progressSyncer.onReceiveMaxBlockHeight(100)

        verify(progressListener).onProgressUpdate(1.0)
        verifyNoMoreInteractions(progressListener)
    }

    @Test
    fun progress_simple() {
        progressSyncer.onInitialBestBlockHeight(0)
        progressSyncer.onReceiveMaxBlockHeight(100)
        progressSyncer.onCurrentBestBlockHeight(23)

        val inOrder = inOrder(progressListener)
        inOrder.verify(progressListener).onProgressUpdate(0.23)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun progress_remoteLongerThanAnnounced() {
        progressSyncer.onInitialBestBlockHeight(0)
        progressSyncer.onReceiveMaxBlockHeight(100)
        progressSyncer.onCurrentBestBlockHeight(200)

        val inOrder = inOrder(progressListener)
        inOrder.verify(progressListener).onProgressUpdate(1.0)
        inOrder.verifyNoMoreInteractions()
    }

}