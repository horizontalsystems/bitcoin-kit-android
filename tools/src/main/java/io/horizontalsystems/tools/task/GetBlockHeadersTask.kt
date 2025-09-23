package io.horizontalsystems.tools.task

import io.horizontalsystems.tools.messages.GetHeadersMessage
import io.horizontalsystems.tools.messages.HeadersMessage
import io.horizontalsystems.tools.messages.IMessage
import io.horizontalsystems.tools.pack.BlockHeader
import io.horizontalsystems.tools.peer.PeerTask


class GetBlockHeadersTask(private val blockLocatorHashes: List<ByteArray>) : PeerTask() {

    var blockHeaders = arrayOf<BlockHeader>()

    override fun start() {
        requester?.let { it.send(
            GetHeadersMessage(
                it.protocolVersion,
                blockLocatorHashes,
                ByteArray(32)
            )
        ) }
        resetTimer()
    }

    override fun handleMessage(message: IMessage): Boolean {
        if (message !is HeadersMessage) {
            return false
        }

        blockHeaders = message.headers
        listener?.onTaskCompleted(this)

        return true
    }

    override fun handleTimeout() {
        listener?.onTaskCompleted(this)
    }
}
