package io.horizontalsystems.bitcoinkit.storage

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import io.horizontalsystems.bitcoinkit.models.BlockchainState
import io.horizontalsystems.bitcoinkit.models.FeeRate

@Database(entities = [FeeRate::class, BlockchainState::class], version = 3, exportSchema = false)
abstract class KitDatabase : RoomDatabase() {

    abstract fun feeRate(): FeeRateDao
    abstract fun blockchainState(): BlockchainStateDao

    companion object {

        @Volatile
        private var instance: KitDatabase? = null

        @Synchronized
        fun getInstance(context: Context, dbName: String): KitDatabase {
            return instance ?: buildDatabase(context, dbName).also { instance = it }
        }

        private fun buildDatabase(context: Context, dbName: String): KitDatabase {
            return Room.databaseBuilder(context, KitDatabase::class.java, dbName)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .addMigrations()
                    .build()
        }
    }
}
