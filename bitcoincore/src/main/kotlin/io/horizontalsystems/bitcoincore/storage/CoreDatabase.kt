package io.horizontalsystems.bitcoincore.storage

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.migration.Migration
import android.content.Context
import io.horizontalsystems.bitcoincore.models.*

@Database(version = 2, exportSchema = false, entities = [
    BlockchainState::class,
    PeerAddress::class,
    BlockHash::class,
    Block::class,
    SentTransaction::class,
    Transaction::class,
    TransactionInput::class,
    TransactionOutput::class,
    PublicKey::class
])

abstract class CoreDatabase : RoomDatabase() {

    abstract val blockchainState: BlockchainStateDao
    abstract val peerAddress: PeerAddressDao
    abstract val blockHash: BlockHashDao
    abstract val block: BlockDao
    abstract val sentTransaction: SentTransactionDao
    abstract val transaction: TransactionDao
    abstract val input: TransactionInputDao
    abstract val output: TransactionOutputDao
    abstract val publicKey: PublicKeyDao

    companion object {

        fun getInstance(context: Context, dbName: String): CoreDatabase {
            return Room.databaseBuilder(context, CoreDatabase::class.java, dbName)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .addMigrations(MIGRATION_1_2)
                    .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Block ADD COLUMN hasTransactions INTEGER DEFAULT 0 NOT NULL")
                database.execSQL("UPDATE Block SET hasTransactions = 1")
            }
        }
    }
}
