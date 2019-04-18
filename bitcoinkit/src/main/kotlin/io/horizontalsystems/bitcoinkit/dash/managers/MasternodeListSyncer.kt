package io.horizontalsystems.bitcoinkit.dash.managers

import io.horizontalsystems.bitcoinkit.dash.tasks.PeerTaskFactory
import io.horizontalsystems.bitcoinkit.dash.tasks.RequestMasternodeListDiffTask
import io.horizontalsystems.bitcoincore.network.peer.IPeerTaskHandler
import io.horizontalsystems.bitcoincore.network.peer.Peer
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup
import io.horizontalsystems.bitcoincore.network.peer.task.PeerTask

class MasternodeListSyncer(private val peerGroup: PeerGroup, val peerTaskFactory: PeerTaskFactory, private val masternodeListManager: MasternodeListManager) : IPeerTaskHandler {

    fun sync(blockHash: ByteArray) {
        addTask(masternodeListManager.baseBlockHash, blockHash)
    }

    override fun handleCompletedTask(peer: Peer, task: PeerTask): Boolean {
        return when (task) {
            is RequestMasternodeListDiffTask -> {
                task.masternodeListDiffMessage?.let { masternodeListDiffMessage ->
                    try {
                        masternodeListManager.updateList(masternodeListDiffMessage)
                    } catch (e: MasternodeListManager.ValidationError) {
                        peer.close(e)

                        addTask(masternodeListDiffMessage.baseBlockHash, masternodeListDiffMessage.blockHash)
                    }
                }
                true
            }
            else -> false
        }
    }

    private fun addTask(baseBlockHash: ByteArray, blockHash: ByteArray) {
        val task = peerTaskFactory.createRequestMasternodeListDiffTask(baseBlockHash, blockHash)
        peerGroup.addTask(task)
    }
}
