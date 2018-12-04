package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import io.horizontalsystems.bitcoinkit.core.publicKey
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.network.*
import io.horizontalsystems.hdwalletkit.HDWallet
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single

class InitialSyncerApi(private val wallet: HDWallet, private val addressSelector: IAddressSelector, network: Network) {

    private val maxHeight: Int = network.checkpointBlock.height
    private val gapLimit = wallet.gapLimit

    private val host = when (network) {
        is MainNet -> "https://btc.horizontalsystems.xyz"
        is TestNet -> "http://btc-testnet.horizontalsystems.xyz"
        is MainNetBitcoinCash -> "https://bch.horizontalsystems.xyz"
        is TestNetBitcoinCash -> "http://bch-testnet.horizontalsystems.xyz"
        else -> "http://btc-testnet.horizontalsystems.xyz"
    }

    private val apiManager = ApiManager(host)

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

    private fun requestApiRecursive(
            external: Boolean,
            index: Int = 0,
            emptyResponsesInRow: Int = 0,
            allKeys: MutableList<PublicKey> = mutableListOf(),
            allBlockResponses: MutableList<BlockResponse> = mutableListOf()
    ): Single<Pair<List<PublicKey>, List<BlockResponse>>> {

        if (emptyResponsesInRow < gapLimit) {
            val publicKey = wallet.publicKey(index, external)
            val addresses = addressSelector.getAddressVariants(publicKey)

            return Flowable.merge(addresses.map { getBlockHashes(it) })
                    .toList()
                    .map { it.flatten().distinct() }
                    .flatMap { blockResponses ->
                        allKeys.add(publicKey)
                        allBlockResponses.addAll(blockResponses.filter { it.height <= maxHeight })

                        requestApiRecursive(external, index + 1, if (blockResponses.isEmpty()) emptyResponsesInRow + 1 else 0, allKeys, allBlockResponses)
                    }
        }

        return Single.just(allKeys to allBlockResponses)
    }

    private fun getBlockHashes(address: String): Flowable<List<BlockResponse>> {
        return Flowable.create<List<BlockResponse>>({ emitter ->
            val list = mutableListOf<BlockResponse>()
            val data = apiManager.getJsonArray("tx/address/$address")

            for (item in data) {
                val tx = item.asObject()
                list.add(BlockResponse(tx["block"].asString(), tx["height"].asInt()))
            }

            emitter.onNext(list)
            emitter.onComplete()
        }, BackpressureStrategy.LATEST)
    }

}
