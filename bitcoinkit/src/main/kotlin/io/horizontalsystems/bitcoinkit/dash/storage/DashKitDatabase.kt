package io.horizontalsystems.bitcoinkit.dash.storage

import android.arch.persistence.room.*
import io.horizontalsystems.bitcoinkit.dash.models.Masternode
import io.horizontalsystems.bitcoinkit.dash.models.MasternodeListState
import io.horizontalsystems.bitcoinkit.models.*
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.storage.KitDatabase

@Database(entities = [
    FeeRate::class,
    BlockchainState::class,
    PeerAddress::class,
    BlockHash::class,
    Block::class,
    SentTransaction::class,
    Transaction::class,
    TransactionInput::class,
    TransactionOutput::class,
    PublicKey::class,
    Masternode::class,
    MasternodeListState::class
], version = 5, exportSchema = false)
abstract class DashKitDatabase : KitDatabase() {
    abstract val masternodeDao: MasternodeDao
    abstract val masternodeListStateDao: MasternodeListStateDao
}

@Dao
interface MasternodeDao {
    @Query("SELECT * FROM Masternode")
    fun getAll(): List<Masternode>

    @Query("DELETE FROM Masternode")
    fun clearAll()

    @Insert
    fun insertAll(masternodes: List<Masternode>)

}

@Dao
interface MasternodeListStateDao {
    @Query("SELECT * FROM MasternodeListState LIMIT 1")
    fun getState(): MasternodeListState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setState(state: MasternodeListState)
}
