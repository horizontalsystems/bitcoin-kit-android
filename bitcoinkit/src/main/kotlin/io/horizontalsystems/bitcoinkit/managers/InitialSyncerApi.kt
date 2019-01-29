package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import io.horizontalsystems.bitcoinkit.core.publicKey
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.network.*
import io.horizontalsystems.hdwalletkit.HDWallet
import io.reactivex.Single

class InitialSyncerApi(private val wallet: HDWallet, private val addressSelector: IAddressSelector, network: Network) {

    private val maxHeight: Int = network.checkpointBlock.height
    private val gapLimit = wallet.gapLimit

    private val host = when (network) {
        is MainNet -> "https://btc.horizontalsystems.xyz/apg"
        is TestNet -> "http://btc-testnet.horizontalsystems.xyz/apg"
        is MainNetBitcoinCash -> "https://bch.horizontalsystems.xyz/apg"
        is TestNetBitcoinCash -> "http://bch-testnet.horizontalsystems.xyz/apg"
        else -> "http://btc-testnet.horizontalsystems.xyz/apg"
    }

    private val apiManager = ApiManager(host)

    @Throws
    fun fetchFromApi(account: Int, external: Boolean): Single<Pair<List<PublicKey>, List<BlockHash>>> =
            Single.create<Pair<List<PublicKey>, List<BlockHash>>> { emitter ->
                val (publicKeys, blockResponses) = requestApiRecursive(account, external)
                try {
                    val accountData = publicKeys to blockResponses.mapNotNull { blockResponse ->
                        try {
                            BlockHash(blockResponse.hash.hexStringToByteArray().reversedArray(), blockResponse.height)
                        } catch (e: NumberFormatException) {
                            null
                        }
                    }
                    emitter.onSuccess(accountData)
                } catch (e: Exception) {
                    emitter.onError(e)
                }
            }

    private fun requestApiRecursive(
            account: Int,
            external: Boolean,
            index: Int = 0,
            emptyResponsesInRow: Int = 0,
            allKeys: MutableList<PublicKey> = mutableListOf(),
            allBlockResponses: MutableList<BlockResponse> = mutableListOf()
    ): Pair<MutableList<PublicKey>, MutableList<BlockResponse>> {

        if (emptyResponsesInRow < gapLimit) {
            val publicKey = wallet.publicKey(account, index, external)
            val addresses = addressSelector.getAddressVariants(publicKey)
            val blockResponses = addresses.map { getBlockHashes(it) }.flatten().distinct()

            allKeys.add(publicKey)
            allBlockResponses.addAll(blockResponses.filter { it.height <= maxHeight })

            return requestApiRecursive(account, external, index + 1, if (blockResponses.isEmpty()) emptyResponsesInRow + 1 else 0, allKeys, allBlockResponses)
        }

        return allKeys to allBlockResponses
    }

    private fun getBlockHashes(address: String): List<BlockResponse> {
        val list = mutableListOf<BlockResponse>()
        val data = apiManager.getJsonArray("tx/address/$address")

        for (item in data) {
            val tx = item.asObject()
            val txBlockHash = tx["block"] ?: continue
            val txBlockHeight = tx["height"] ?: continue

            list.add(BlockResponse(txBlockHash.asString(), txBlockHeight.asInt()))
        }
        return list
    }

}
