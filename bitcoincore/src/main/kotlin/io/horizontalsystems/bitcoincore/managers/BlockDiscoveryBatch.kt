package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.Wallet
import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.reactivex.Single

class BlockDiscoveryBatch(private val wallet: Wallet, private val blockHashFetcher: BlockHashFetcher, private val maxHeight: Int) : IBlockDiscovery {

    private val gapLimit = wallet.gapLimit

    override fun discoverBlockHashes(account: Int, external: Boolean): Single<Pair<List<PublicKey>, List<BlockHash>>> {
        return Single.create { emitter ->
            try {
                emitter.onSuccess(fetchRecursive(account, external))
            } catch (e: Exception) {
                emitter.tryOnError(e)
            }
        }
    }

    private fun fetchRecursive(account: Int, external: Boolean, publicKeys: List<PublicKey> = listOf(), blockHashes: List<BlockHash> = listOf(), prevCount: Int = 0, prevLastUsedIndex: Int = -1, startIndex: Int = 0): Pair<List<PublicKey>, List<BlockHash>> {
        val count = gapLimit - prevCount + prevLastUsedIndex + 1

        val newPublicKeys = List(count) {
            wallet.publicKey(account, it + startIndex, external)
        }

        val (newBlockHashes, lastUsedIndex) = blockHashFetcher.getBlockHashes(newPublicKeys)

        val resultBlockHashes = blockHashes + newBlockHashes.filter { it.height <= maxHeight }
        val resultPublicKeys = publicKeys + newPublicKeys

        return when {
            // found all unused keys
            lastUsedIndex < 0 -> Pair(resultPublicKeys, resultBlockHashes)
            // found some used keys
            else -> fetchRecursive(account, external, resultPublicKeys, resultBlockHashes, count, lastUsedIndex, startIndex + count)
        }
    }

}
