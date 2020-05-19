package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.core.Wallet
import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.reactivex.Single

class BlockDiscoveryBatch(
        private val wallet: Wallet,
        private val blockHashFetcher: BlockHashFetcher,
        private val maxHeight: Int
) : IBlockDiscovery {

    private val gapLimit = wallet.gapLimit

    override fun discoverBlockHashes(account: Int): Single<Pair<List<PublicKey>, List<BlockHash>>> {
        return Single.create { emitter ->
            try {
                emitter.onSuccess(fetchRecursive(account))
            } catch (e: Exception) {
                emitter.tryOnError(e)
            }
        }
    }

    private fun fetchRecursive(
            account: Int,
            blockHashes: List<BlockHash> = listOf(),
            externalBatchInfo: KeyBlockHashBatchInfo = KeyBlockHashBatchInfo(),
            internalBatchInfo: KeyBlockHashBatchInfo = KeyBlockHashBatchInfo()
    ): Pair<List<PublicKey>, List<BlockHash>> {

        val externalCount = gapLimit - externalBatchInfo.prevCount + externalBatchInfo.prevLastUsedIndex + 1
        val internalCount = gapLimit - internalBatchInfo.prevCount + internalBatchInfo.prevLastUsedIndex + 1

        val externalNewKeys = wallet.publicKeys(account, externalBatchInfo.startIndex until externalBatchInfo.startIndex + externalCount, true)
        val internalNewKeys = wallet.publicKeys(account, internalBatchInfo.startIndex until internalBatchInfo.startIndex + internalCount, false)

        val externalPublicKeys = externalBatchInfo.publicKeys + externalNewKeys
        val internalPublicKeys = internalBatchInfo.publicKeys + internalNewKeys

        val fetchResponse = blockHashFetcher.getBlockHashes(externalNewKeys, internalNewKeys)
        val resultBlockHashes = blockHashes + fetchResponse.blockHashes.filter { it.height <= maxHeight }

        return when {
            // found all unused keys
            fetchResponse.externalLastUsedIndex < 0 && fetchResponse.internalLastUsedIndex < 0 -> {
                Pair(externalPublicKeys + internalPublicKeys, resultBlockHashes)
            }
            // found some used keys
            else -> {
                val externalBatch = KeyBlockHashBatchInfo(externalPublicKeys, externalCount, fetchResponse.externalLastUsedIndex, externalBatchInfo.startIndex + externalCount)
                val internalBatch = KeyBlockHashBatchInfo(internalPublicKeys, internalCount, fetchResponse.internalLastUsedIndex, internalBatchInfo.startIndex + internalCount)
                fetchRecursive(account, resultBlockHashes, externalBatch, internalBatch)
            }
        }
    }

    private data class KeyBlockHashBatchInfo(
            var publicKeys: List<PublicKey> = listOf(),
            var prevCount: Int = 0,
            var prevLastUsedIndex: Int = -1,
            var startIndex: Int = 0
    )

}
