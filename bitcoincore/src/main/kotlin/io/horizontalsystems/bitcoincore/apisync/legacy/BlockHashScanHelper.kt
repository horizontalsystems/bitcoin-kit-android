package io.horizontalsystems.bitcoincore.apisync.legacy

import io.horizontalsystems.bitcoincore.apisync.model.AddressItem

interface IBlockHashScanHelper {
    fun lastUsedIndex(addresses: List<List<String>>, addressItems: List<AddressItem>): Int
}

class BlockHashScanHelper : IBlockHashScanHelper {

    override fun lastUsedIndex(addresses: List<List<String>>, addressItems: List<AddressItem>): Int {
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

class WatchAddressBlockHashScanHelper : IBlockHashScanHelper {

    override fun lastUsedIndex(addresses: List<List<String>>, addressItems: List<AddressItem>): Int = -1

}
