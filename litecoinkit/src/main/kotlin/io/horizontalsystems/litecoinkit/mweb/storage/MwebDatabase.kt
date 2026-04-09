package io.horizontalsystems.litecoinkit.mweb.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.horizontalsystems.litecoinkit.mweb.storage.entities.MwebOutput
import io.horizontalsystems.litecoinkit.mweb.storage.entities.MwebSyncState
import io.horizontalsystems.litecoinkit.mweb.storage.entities.MwebWalletOutput

@Database(
    entities = [MwebOutput::class, MwebWalletOutput::class, MwebSyncState::class],
    version = 3,
    exportSchema = false
)
abstract class MwebDatabase : RoomDatabase() {
    abstract val mwebDao: MwebDao

    companion object {
        fun getInstance(context: Context, dbName: String): MwebDatabase =
            Room.databaseBuilder(context, MwebDatabase::class.java, dbName)
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
    }
}