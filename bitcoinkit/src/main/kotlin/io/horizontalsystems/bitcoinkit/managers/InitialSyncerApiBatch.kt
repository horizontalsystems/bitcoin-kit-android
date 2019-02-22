package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.hdwalletkit.HDWallet
import io.reactivex.Single

class InitialSyncerApiBatch(private val wallet: Wallet, private val blockHashFetcher: BlockHashFetcher, network: Network) {

    private val maxHeight: Int = network.checkpointBlock.height
    private val gapLimit = wallet.gapLimit

    @Throws
    fun fetchFromApi(account: Int, external: Boolean): Single<Pair<List<PublicKey>, List<BlockHash>>> {
        return Single.create<Pair<List<PublicKey>, List<BlockHash>>> { emitter ->
            try {
                emitter.onSuccess(fetch(account, external))
            } catch (e: Exception) {
                emitter.tryOnError(e)
            }
        }
    }

    private fun fetch(account: Int, external: Boolean, publicKeys: List<PublicKey> = listOf(), blockHashes: List<BlockHash> = listOf(), prevCount: Int = 0, prevLastUsedIndex: Int = -1, startIndex: Int = 0): Pair<List<PublicKey>, List<BlockHash>> {
        val count = gapLimit - prevCount + prevLastUsedIndex + 1

        val newPublicKeys = List(count) {
            wallet.publicKey(account, it + startIndex, external)
        }

        val (newBlockHashes, lastUsedIndex) = blockHashFetcher.getBlockHashes(newPublicKeys)

        val resultBlockHashes = blockHashes + newBlockHashes.filter { it.height <= maxHeight }
        val resultPublicKeys = publicKeys + newPublicKeys

        if (lastUsedIndex < 0) {
            // found all unused keys
            return Pair(resultPublicKeys, resultBlockHashes)
        } else {
            return fetch(account, external, resultPublicKeys, resultBlockHashes, count, lastUsedIndex, startIndex + count)
            // found some used keys
        }
    }
}


class Wallet(private val hdWallet: HDWallet) {

    val gapLimit = hdWallet.gapLimit

    fun publicKey(account: Int, index: Int, external: Boolean): PublicKey {
        val hdPubKey = hdWallet.hdPublicKey(account, index, external)
        return PublicKey(account, index, hdPubKey.external, hdPubKey.publicKey, hdPubKey.publicKeyHash)
    }
}

class BlockHashFetcher(private val addressSelector: IAddressSelector, private val apiManager: ApiManager, private val helper: BlockHashFetcherHelper) {

    fun getBlockHashes(publicKeys: List<PublicKey>): Pair<List<BlockHash>, Int> {
        val addresses = publicKeys.map {
            addressSelector.getAddressVariants(it)
        }

        val transactions = apiManager.getTransactions(addresses.flatten())

        if (transactions.isEmpty()) {
            return Pair(listOf(), -1)
        }

        val lastUsedIndex = helper.lastUsedIndex(addresses, transactions.map { it.txOutputs }.flatten())

        val blockHashes = transactions.map {
            BlockHash(it.blockHash, it.blockHeight)
        }

        return Pair(blockHashes, lastUsedIndex)
    }

}

class BlockHashFetcherHelper {

    fun lastUsedIndex(addresses: List<List<String>>, outputs: List<BCoinTransactionOutput>): Int {
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