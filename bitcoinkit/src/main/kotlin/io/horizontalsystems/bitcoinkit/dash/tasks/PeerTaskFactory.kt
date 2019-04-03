package io.horizontalsystems.bitcoinkit.dash.tasks

class PeerTaskFactory {

    fun createRequestMasternodeListDiffTask(baseBlockHash: ByteArray, blockHash: ByteArray): RequestMasternodeListDiffTask {
        return RequestMasternodeListDiffTask(baseBlockHash, blockHash)
    }

}
