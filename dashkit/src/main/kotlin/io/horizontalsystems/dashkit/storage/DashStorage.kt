package io.horizontalsystems.dashkit.storage

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.storage.Storage
import io.horizontalsystems.dashkit.IDashStorage
import io.horizontalsystems.dashkit.models.Masternode
import io.horizontalsystems.dashkit.models.MasternodeListState

class DashStorage(private val dashStore: DashKitDatabase, private val coreStorage: Storage) : IDashStorage {

    override fun getBlock(blockHash: ByteArray): Block? {
        return coreStorage.getBlock(blockHash)
    }

    override var masternodes: List<Masternode>
        get() = dashStore.masternodeDao.getAll()
        set(value) {
            dashStore.masternodeDao.clearAll()
            dashStore.masternodeDao.insertAll(value)
        }

    override var masternodeListState: MasternodeListState?
        get() = dashStore.masternodeListStateDao.getState()
        set(value) {
            value?.let {
                dashStore.masternodeListStateDao.setState(value)
            }
        }
}
