package io.horizontalsystems.litecoinkit.mweb.network.tasks

import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.peer.task.PeerTask
import io.horizontalsystems.litecoinkit.mweb.network.messages.MwebTransactionMessage
import java.util.concurrent.TimeUnit

/**
 * Sends a serialized MWEB transaction to a peer (fire-and-forget).
 *
 * Completion is confirmed by the next MWEB UTXO sync session: the spent input
 * commitments will be absent from the UTXO set returned by the peer.
 */
class SendMwebTransactionTask(private val mwebTxBytes: ByteArray) : PeerTask() {

    init {
        allowedIdleTime = TimeUnit.SECONDS.toMillis(10)
    }

    override fun start() {
        requester?.send(MwebTransactionMessage(mwebTxBytes))
        listener?.onTaskCompleted(this)
    }

    override fun handleMessage(message: IMessage): Boolean = false

    override fun handleTimeout() {
        listener?.onTaskCompleted(this)
    }
}
