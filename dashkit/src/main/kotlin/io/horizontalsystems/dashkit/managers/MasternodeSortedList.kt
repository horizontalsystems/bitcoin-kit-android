package io.horizontalsystems.dashkit.managers

import io.horizontalsystems.dashkit.models.Masternode

class MasternodeSortedList {
    private val masternodeList = mutableListOf<Masternode>()

    val masternodes: List<Masternode>
        get() = masternodeList.sorted()

    fun add(masternodes: List<Masternode>) {
        masternodeList.removeAll(masternodes)
        masternodeList.addAll(masternodes)
    }

    fun remove(proRegTxHashes: List<ByteArray>) {
        proRegTxHashes.forEach { hash ->
            val index = masternodeList.indexOfFirst { masternode ->
                masternode.proRegTxHash.contentEquals(hash)
            }

            if (index != -1) {
                masternodeList.removeAt(index)
            }
        }
    }

    fun removeAll() {
        masternodeList.clear()
    }

}
