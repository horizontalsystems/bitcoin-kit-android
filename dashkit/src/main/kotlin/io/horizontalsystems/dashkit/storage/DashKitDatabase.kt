package io.horizontalsystems.dashkit.storage

import android.arch.persistence.room.*
import android.content.Context
import io.horizontalsystems.dashkit.models.Masternode
import io.horizontalsystems.dashkit.models.MasternodeListState

@Database(version = 1, exportSchema = false, entities = [
    Masternode::class,
    MasternodeListState::class
])

abstract class DashKitDatabase : RoomDatabase() {
    abstract val masternodeDao: MasternodeDao
    abstract val masternodeListStateDao: MasternodeListStateDao

    companion object {

        @Volatile
        private var instance: DashKitDatabase? = null

        @Synchronized
        fun getInstance(context: Context, dbName: String): DashKitDatabase {
            return instance ?: buildDatabase(context, dbName).also { instance = it }
        }

        private fun buildDatabase(context: Context, dbName: String): DashKitDatabase {
            return Room.databaseBuilder(context, DashKitDatabase::class.java, dbName)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
        }
    }
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
