package io.horizontalsystems.dashkit

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.dashkit.models.Masternode
import io.horizontalsystems.dashkit.models.MasternodeListState

interface IDashStorage : IStorage {
    var masternodes: List<Masternode>
    var masternodeListState: MasternodeListState?
}