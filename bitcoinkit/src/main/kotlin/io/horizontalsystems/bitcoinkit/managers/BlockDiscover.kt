package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import io.horizontalsystems.bitcoinkit.core.publicKey
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.network.NetworkParameters
import io.horizontalsystems.bitcoinkit.utils.AddressConverter
import io.horizontalsystems.hdwalletkit.HDWallet
import io.reactivex.Observable

class BlockDiscover(private val hdWallet: HDWallet,
                    private val apiManager: ApiManager,
                    network: NetworkParameters,
                    private val addressConverter: AddressConverter) {

    private val maxHeight: Int = network.checkpointBlock.height
    private val gapLimit = hdWallet.gapLimit

    @Throws
    fun fetchFromApi(external: Boolean): Observable<Pair<List<PublicKey>, List<BlockHash>>> {
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

    private fun requestApiRecursive(external: Boolean, index: Int = 0, emptyResponsesInRow: Int = 0, allKeys: MutableList<PublicKey> = mutableListOf(), allBlockResponses: MutableList<BlockResponse> = mutableListOf()): Observable<Pair<List<PublicKey>, List<BlockResponse>>> {
        return if (emptyResponsesInRow < gapLimit) {
            val publicKey = hdWallet.publicKey(index, external)

            apiManager
                    .getBlockHashes(addressConverter.convert(publicKey.publicKey).toString())
                    .flatMap { blockResponses ->

                        allKeys.add(publicKey)
                        allBlockResponses.addAll(blockResponses.filter { it.height < maxHeight })

                        requestApiRecursive(external, index + 1, if (blockResponses.isEmpty()) emptyResponsesInRow + 1 else 0, allKeys, allBlockResponses)
                    }
        } else {
            Observable.just(allKeys to allBlockResponses)
        }
    }

}
