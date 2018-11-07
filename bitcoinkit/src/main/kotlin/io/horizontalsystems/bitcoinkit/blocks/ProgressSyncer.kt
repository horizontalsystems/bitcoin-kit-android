package io.horizontalsystems.bitcoinkit.blocks

import io.horizontalsystems.bitcoinkit.network.PeerGroup

class ProgressSyncer(private val listener: Listener) : PeerGroup.LastBlockHeightListener, BlockSyncer.Listener {

    interface Listener {
        fun onProgressUpdate(progress: Double)
    }

    private var initialBestBlockHeight = 0
    private var currentBestBlockHeight = 0
    private var maxBlockHeight = 0

    override fun onInitialBestBlockHeight(height: Int) {
        initialBestBlockHeight = height
        currentBestBlockHeight = height
    }

    override fun onCurrentBestBlockHeight(height: Int) {
        currentBestBlockHeight = Math.max(currentBestBlockHeight, height)
        notifyListener()
    }

    override fun onReceiveMaxBlockHeight(height: Int) {
        maxBlockHeight = height
        notifyListener()
    }

    private fun notifyListener() {
        val blocksDownloaded = currentBestBlockHeight - initialBestBlockHeight
        val allBlocksToDownload = maxBlockHeight - initialBestBlockHeight

        val progress = when {
            allBlocksToDownload <= 0 -> 1.0
            else -> blocksDownloaded / allBlocksToDownload.toDouble()
        }

        listener.onProgressUpdate(Math.min(progress, 1.0))
    }
}
