package io.horizontalsystems.dashkit.managers

import io.horizontalsystems.dashkit.models.Quorum

class QuorumSortedList {
    private val quorumList = mutableListOf<Quorum>()

    val quorums: List<Quorum>
        get() = quorumList.sorted()


    fun add(quorums: List<Quorum>) {
        quorumList.removeAll(quorums)
        quorumList.addAll(quorums)
    }

    fun remove(quorums: List<Pair<Int, ByteArray>>) {
        quorums.forEach { (type, quorumHash) ->
            val index = quorumList.indexOfFirst { quorum ->
                quorum.type == type && quorum.quorumHash.contentEquals(quorumHash)
            }

            if (index != -1) {
                quorumList.removeAt(index)
            }
        }
    }

    fun removeAll() {
        quorumList.clear()
    }

}
