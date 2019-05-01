package io.horizontalsystems.dashkit.storage

import android.arch.persistence.room.*
import io.horizontalsystems.dashkit.models.Masternode
import io.horizontalsystems.dashkit.models.MasternodeListState

@Database(version = 1, exportSchema = false, entities = [
    Masternode::class,
    MasternodeListState::class
])

abstract class DashKitDatabase : RoomDatabase() {
    abstract val masternodeDao: MasternodeDao
    abstract val masternodeListStateDao: MasternodeListStateDao
}

@Dao
interface MasternodeDao {
    @Insert
    fun insertAll(masternodes: List<Masternode>)

    @Query("SELECT * FROM Masternode")
    fun getAll(): List<Masternode>

    @Query("DELETE FROM Masternode")
    fun clearAll()
}

@Dao
interface MasternodeListStateDao {
    @Query("SELECT * FROM MasternodeListState LIMIT 1")
    fun getState(): MasternodeListState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setState(state: MasternodeListState)
}
