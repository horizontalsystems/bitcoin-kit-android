package io.horizontalsystems.bitcoinkit.dash

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.dash.models.Masternode
import io.horizontalsystems.bitcoinkit.dash.models.MasternodeListState

interface IDashStorage : IStorage {
    var masternodes: List<Masternode>
    var masternodeListState: MasternodeListState?
}