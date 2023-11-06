package io.horizontalsystems.bitcoincore.apisync.legacy

import io.horizontalsystems.bitcoincore.apisync.model.AddressItem

class BlockHashScanHelper {

    fun lastUsedIndex(addresses: List<List<String>>, addressItems: List<AddressItem>): Int {
        val searchAddressStrings = addressItems.map { it.address }
        val searchScriptStrings = addressItems.map { it.script }

        for (i in addresses.size - 1 downTo 0) {
            addresses[i].forEach { address ->
                if (searchAddressStrings.contains(address) || searchScriptStrings.any { script -> script.contains(address) }) {
                    return i
                }
            }
        }

        return -1
    }

}
