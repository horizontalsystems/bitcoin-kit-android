package io.horizontalsystems.dashkit.storage

import io.horizontalsystems.bitcoincore.storage.Storage
import io.horizontalsystems.dashkit.IDashStorage
import io.horizontalsystems.dashkit.models.Masternode
import io.horizontalsystems.dashkit.models.MasternodeListState

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