package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.core.hexStringToByteArray
import bitcoin.wallet.kit.hdwallet.HDWallet
import bitcoin.wallet.kit.hdwallet.PublicKey
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.network.NetworkParameters
import io.reactivex.Observable

class BlockDiscover(private val hdWallet: HDWallet, private val apiManager: ApiManager, network: NetworkParameters) {

    private val maxHeight: Int = network.checkpointBlock.height
    private val gapLimit = hdWallet.gapLimit

    @Throws
    fun fetchFromApi(external: Boolean): Observable<Pair<List<PublicKey>, List<Block>>> {
        return requestApiRecursive(external)
                .map { (publicKeys, blockResponses) ->
                    publicKeys to blockResponses.mapNotNull { blockResponse ->
                        try {
                            Block().apply {
                                headerHash = blockResponse.hash.hexStringToByteArray().reversedArray()
                                height = blockResponse.height
                            }
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
                    .getBlockHashes(publicKey.address)
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
