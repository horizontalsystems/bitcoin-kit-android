package io.horizontalsystems.litecoinkit.mweb.network.tasks

import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.peer.task.PeerTask
import io.horizontalsystems.litecoinkit.mweb.network.messages.GetMwebUtxosMessage
import io.horizontalsystems.litecoinkit.mweb.network.messages.MwebRawOutput
import io.horizontalsystems.litecoinkit.mweb.network.messages.MwebUtxosMessage
import java.util.concurrent.TimeUnit

/**
 * Requests MWEB UTXOs from a peer for a given block starting at [startLeafIndex].
 *
 * On completion: [receivedOutputs] contains the outputs returned by the peer.
 */
class GetMwebUtxosTask(
    val blockHash: ByteArray,
    val startLeafIndex: Long,
    private val numUtxos: Long = PAGE_SIZE
) : PeerTask() {

    var receivedOutputs: List<MwebRawOutput> = emptyList()
        private set

    override fun start() {
        requester?.send(GetMwebUtxosMessage(blockHash, startLeafIndex, numUtxos))
        allowedIdleTime = TimeUnit.SECONDS.toMillis(30)
        resetTimer()
    }

    override fun handleMessage(message: IMessage): Boolean {
        if (message !is MwebUtxosMessage) return false
        if (!message.blockHash.contentEquals(blockHash)) return false
        receivedOutputs = message.outputs
        listener?.onTaskCompleted(this)
        return true
    }

    override fun handleTimeout() {
        // Complete even with no data so we don't stall the peer
        listener?.onTaskCompleted(this)
    }

    companion object {
        const val PAGE_SIZE = 1000L
    }
}