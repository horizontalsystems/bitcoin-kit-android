package io.horizontalsystems.dashkit.instantsend

import io.horizontalsystems.dashkit.models.Masternode

class QuorumMasternode(val quorumHash: ByteArray, val masternode: Masternode) : Comparable<QuorumMasternode> {

    override fun compareTo(other: QuorumMasternode): Int {
        for (i in 0 until quorumHash.size) {
            val b1: Int = quorumHash[i].toInt() and 0xff
            val b2: Int = other.quorumHash[i].toInt() and 0xff

            val res = b1.compareTo(b2)
            if (res != 0) return res
        }

        return 0
    }
}