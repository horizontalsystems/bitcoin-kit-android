package io.horizontalsystems.dashkit

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.dashkit.models.Masternode
import io.horizontalsystems.dashkit.models.MasternodeListState

interface IDashStorage {
    fun getBlock(blockHash: ByteArray): Block?

    var masternodes: List<Masternode>
    var masternodeListState: MasternodeListState?
}
