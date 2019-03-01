package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.reactivex.Single

interface IBlockDiscovery {
    fun discoverBlockHashes(account: Int, external: Boolean): Single<Pair<List<PublicKey>, List<BlockHash>>>
}
