package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import io.horizontalsystems.bitcoinkit.core.publicKey
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.hdwalletkit.HDWallet
import io.reactivex.Single

class BlockDiscover(private val hdWallet: HDWallet,
                    private val apiManager: IApiManager,
                    network: Network) {

    private val maxHeight: Int = network.checkpointBlock.height
    private val gapLimit = hdWallet.gapLimit

    @Throws
    fun fetchFromApi(external: Boolean): Single<Pair<List<PublicKey>, List<BlockHash>>> {
        return requestApiRecursive(external)
                .map { (publicKeys, blockResponses) ->
                    publicKeys to blockResponses.mapNotNull { blockResponse ->
                        try {
                            BlockHash(blockResponse.hash.hexStringToByteArray().reversedArray(), blockResponse.height)
                        } catch (e: NumberFormatException) {
                            null
                        }
                    }
                }
    }

    private fun requestApiRecursive(external: Boolean, index: Int = 0, emptyResponsesInRow: Int = 0, allKeys: MutableList<PublicKey> = mutableListOf(), allBlockResponses: MutableList<BlockResponse> = mutableListOf()): Single<Pair<List<PublicKey>, List<BlockResponse>>> {
        return if (emptyResponsesInRow < gapLimit) {
            val publicKey = hdWallet.publicKey(index, external)

            apiManager
                    .getBlockHashes(publicKey)
                    .flatMap { blockResponses ->

                        allKeys.add(publicKey)
                        allBlockResponses.addAll(blockResponses.filter { it.height <= maxHeight })

                        requestApiRecursive(external, index + 1, if (blockResponses.isEmpty()) emptyResponsesInRow + 1 else 0, allKeys, allBlockResponses)
                    }
        } else {
            Single.just(allKeys to allBlockResponses)
        }
    }

}
