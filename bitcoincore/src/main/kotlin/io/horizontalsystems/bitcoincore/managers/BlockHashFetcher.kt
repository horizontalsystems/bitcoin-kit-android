package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.IApiSyncListener
import io.horizontalsystems.bitcoincore.core.IInitialSyncApi
import io.horizontalsystems.bitcoincore.extensions.toReversedByteArray
import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.models.PublicKey

class BlockHashFetcher(
        private val restoreKeyConverter: IRestoreKeyConverter,
        private val initialSyncerApi: IInitialSyncApi,
        private val helper: BlockHashFetcherHelper
) {

    var listener: IApiSyncListener? = null

    fun getBlockHashes(externalKeys: List<PublicKey>, internalKeys: List<PublicKey>): BlockHashesResponse {
        val externalAddresses = externalKeys.map {
            restoreKeyConverter.keysForApiRestore(it)
        }
        val internalAddresses = internalKeys.map {
            restoreKeyConverter.keysForApiRestore(it)
        }
        val allAddresses = externalAddresses.flatten() + internalAddresses.flatten()
        val transactions = initialSyncerApi.getTransactions(allAddresses)

        if (transactions.isEmpty()) {
            return BlockHashesResponse(listOf(), -1, -1)
        }

        listener?.onTransactionsFound(transactions.size)

        val outputs = transactions.flatMap { it.txOutputs }
        val externalLastUsedIndex = helper.lastUsedIndex(externalAddresses, outputs)
        val internalLastUsedIndex = helper.lastUsedIndex(internalAddresses, outputs)

        val blockHashes = transactions.map {
            BlockHash(it.blockHash.toReversedByteArray(), it.blockHeight, 0)
        }

        return BlockHashesResponse(blockHashes, externalLastUsedIndex, internalLastUsedIndex)
    }

    data class BlockHashesResponse(
            val blockHashes: List<BlockHash>,
            val externalLastUsedIndex: Int,
            val internalLastUsedIndex: Int
    )
}

class BlockHashFetcherHelper {

    fun lastUsedIndex(addresses: List<List<String>>, outputs: List<TransactionOutputItem>): Int {
        val searchAddressStrings = outputs.map { it.address }
        val searchScriptStrings = outputs.map { it.script }

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

data class TransactionItem(val blockHash: String, val blockHeight: Int, val txOutputs: List<TransactionOutputItem>)
data class TransactionOutputItem(val script: String, val address: String)
