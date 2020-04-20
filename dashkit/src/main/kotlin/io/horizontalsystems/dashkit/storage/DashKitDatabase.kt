package io.horizontalsystems.dashkit.storage

import androidx.room.*
import android.content.Context
import io.horizontalsystems.dashkit.models.*

@Database(version = 2, exportSchema = false, entities = [
    Masternode::class,
    Quorum::class,
    MasternodeListState::class,
    InstantTransactionInput::class,
    InstantTransactionHash::class
])

abstract class DashKitDatabase : RoomDatabase() {
    abstract val instantTransactionHashDao: InstantTransactionHashDao
    abstract val masternodeDao: MasternodeDao
    abstract val quorumDao: QuorumDao
    abstract val masternodeListStateDao: MasternodeListStateDao
    abstract val instantTransactionInputDao: InstantTransactionInputDao

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
interface InstantTransactionHashDao {
    @Query("SELECT * FROM InstantTransactionHash")
    fun getAll(): List<InstantTransactionHash>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(instantTransactionHash: InstantTransactionHash)

}

@Dao
interface InstantTransactionInputDao {
    @Query("SELECT * FROM InstantTransactionInput WHERE txHash = :txHash")
    fun getByTx(txHash: ByteArray): List<InstantTransactionInput>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: InstantTransactionInput)

    @Query("DELETE FROM InstantTransactionInput WHERE txHash = :txHash")
    fun deleteByTx(txHash: ByteArray)
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
interface QuorumDao {
    @Insert
    fun insertAll(masternodes: List<Quorum>)

    @Query("SELECT * FROM Quorum")
    fun getAll(): List<Quorum>

    @Query("SELECT * FROM Quorum WHERE type = :type")
    fun getByType(type: Int): List<Quorum>

    @Query("DELETE FROM Quorum")
    fun clearAll()
}

@Dao
interface MasternodeListStateDao {
    @Query("SELECT * FROM MasternodeListState LIMIT 1")
    fun getState(): MasternodeListState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setState(state: MasternodeListState)
}
