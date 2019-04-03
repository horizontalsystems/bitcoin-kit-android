package io.horizontalsystems.bitcoinkit.dash.tasks

import io.horizontalsystems.bitcoinkit.dash.messages.GetMasternodeListDiffMessage
import io.horizontalsystems.bitcoinkit.dash.messages.MasternodeListDiffMessage
import io.horizontalsystems.bitcoinkit.network.messages.Message
import io.horizontalsystems.bitcoinkit.network.peer.task.PeerTask

class RequestMasternodeListDiffTask(private val baseBlockHash: ByteArray, private val blockHash: ByteArray) : PeerTask() {

    var masternodeListDiffMessage: MasternodeListDiffMessage? = null

    override fun start() {
        val message = GetMasternodeListDiffMessage()
        message.baseBlockHash = baseBlockHash
        message.blockHash = blockHash

        requester?.sendMessage(message)
    }

    override fun handleMessage(message: Message): Boolean {
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
