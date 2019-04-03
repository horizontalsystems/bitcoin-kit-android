package io.horizontalsystems.bitcoinkit.dash.storage

import io.horizontalsystems.bitcoinkit.dash.IDashStorage
import io.horizontalsystems.bitcoinkit.dash.models.Masternode
import io.horizontalsystems.bitcoinkit.dash.models.MasternodeListState
import io.horizontalsystems.bitcoinkit.storage.Storage

class DashStorage(override val store: DashKitDatabase) : Storage(store), IDashStorage {

    override var masternodes: List<Masternode>
        get() = store.masternodeDao.getAll()
        set(value) {
            store.masternodeDao.clearAll()
            store.masternodeDao.insertAll(value)
        }
    override var masternodeListState: MasternodeListState?
        get() = store.masternodeListStateDao.getState()
        set(value) {
            value?.let {
                store.masternodeListStateDao.setState(value)
            }
        }
}