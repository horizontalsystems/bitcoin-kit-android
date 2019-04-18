package io.horizontalsystems.bitcoinkit.dash.tasks

import io.horizontalsystems.bitcoinkit.dash.messages.GetMasternodeListDiffMessage
import io.horizontalsystems.bitcoinkit.dash.messages.MasternodeListDiffMessage
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.peer.task.PeerTask

class RequestMasternodeListDiffTask(private val baseBlockHash: ByteArray, private val blockHash: ByteArray) : PeerTask() {

    var masternodeListDiffMessage: MasternodeListDiffMessage? = null

    override fun start() {
        val message = GetMasternodeListDiffMessage(baseBlockHash, blockHash)

        requester?.sendMessage(message)
    }

    override fun handleMessage(message: IMessage): Boolean {
        if (message is MasternodeListDiffMessage
                && message.baseBlockHash.contentEquals(baseBlockHash)
                && message.blockHash.contentEquals(blockHash)) {

            masternodeListDiffMessage = message

            listener?.onTaskCompleted(this)

            return true
        }

        return false
    }
}
