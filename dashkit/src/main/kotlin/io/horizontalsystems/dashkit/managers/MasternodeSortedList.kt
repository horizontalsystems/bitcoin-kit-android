package io.horizontalsystems.dashkit.managers

import io.horizontalsystems.dashkit.models.Masternode

class MasternodeSortedList {
    private val masternodeSet = mutableListOf<Masternode>()

    val masternodes: List<Masternode>
        get() = masternodeSet.sorted()


    fun add(masternodes: List<Masternode>) {
        masternodeSet.addAll(masternodes)
    }

    fun remove(proRegTxHashes: List<ByteArray>) {
        proRegTxHashes.forEach { hash ->
            val index = masternodeSet.indexOfFirst { masternode ->
                masternode.proRegTxHash.contentEquals(hash)
            }

            if (index != -1) {
                masternodeSet.removeAt(index)
            }
        }
    }

    fun removeAll() {
        masternodeSet.clear()
    }

}
