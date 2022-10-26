package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.reactivex.Single

interface IBlockDiscovery {
    fun discoverBlockHashes(): Single<Pair<List<PublicKey>, List<BlockHash>>>
}
