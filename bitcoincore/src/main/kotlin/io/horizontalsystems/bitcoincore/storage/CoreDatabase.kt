package io.horizontalsystems.bitcoincore.storage

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import android.content.Context
import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.bitcoincore.storage.migrations.Migration_10_11
import io.horizontalsystems.bitcoincore.storage.migrations.Migration_11_12

@Database(version = 12, exportSchema = false, entities = [
    BlockchainState::class,
    PeerAddress::class,
    BlockHash::class,
    Block::class,
    SentTransaction::class,
    Transaction::class,
    TransactionInput::class,
    TransactionOutput::class,
    PublicKey::class,
    InvalidTransaction::class
])
@TypeConverters(ScriptTypeConverter::class)
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
    abstract val invalidTransaction: InvalidTransactionDao

    companion object {

        fun getInstance(context: Context, dbName: String): CoreDatabase {
            return Room.databaseBuilder(context, CoreDatabase::class.java, dbName)
                    .allowMainThreadQueries()
                    .addMigrations(
                            Migration_11_12,
                            Migration_10_11,
                            add_rawTransaction_to_Transaction,
                            add_conflictingTxHash_to_Transaction,
                            add_table_InvalidTransaction,
                            update_transaction_output,
                            update_block_timestamp,
                            add_hasTransaction_to_Block,
                            add_connectionTime_to_PeerAddress
                    )
                    .fallbackToDestructiveMigration()
                    .build()
        }

        private val add_rawTransaction_to_Transaction = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `Transaction` ADD COLUMN `rawTransaction` TEXT")
                database.execSQL("ALTER TABLE `InvalidTransaction` ADD COLUMN `rawTransaction` TEXT")
            }
        }

        private val add_conflictingTxHash_to_Transaction = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `Transaction` ADD COLUMN `conflictingTxHash` BLOB")
                database.execSQL("ALTER TABLE `InvalidTransaction` ADD COLUMN `conflictingTxHash` BLOB")
            }
        }

        private val add_table_InvalidTransaction = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `InvalidTransaction` (`hash` BLOB NOT NULL, `blockHash` BLOB, `version` INTEGER NOT NULL, `lockTime` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `order` INTEGER NOT NULL, `isMine` INTEGER NOT NULL, `isOutgoing` INTEGER NOT NULL, `segwit` INTEGER NOT NULL, `status` INTEGER NOT NULL, `serializedTxInfo` TEXT NOT NULL, PRIMARY KEY(`hash`))")
                database.execSQL("ALTER TABLE SentTransaction ADD COLUMN sendSuccess INTEGER DEFAULT 0 NOT NULL")
                database.execSQL("ALTER TABLE `Transaction` ADD COLUMN serializedTxInfo TEXT DEFAULT '' NOT NULL")
            }
        }

        private val update_transaction_output = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE TransactionOutput ADD COLUMN `pluginId` INTEGER")
                database.execSQL("ALTER TABLE TransactionOutput ADD COLUMN `pluginData` TEXT")
            }
        }

        private val update_block_timestamp = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("UPDATE Block SET block_timestamp = 1559256184 WHERE height = 578592 AND block_timestamp = 1559277784")
            }
        }

        private val add_hasTransaction_to_Block = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Block ADD COLUMN hasTransactions INTEGER DEFAULT 0 NOT NULL")
                database.execSQL("UPDATE Block SET hasTransactions = 1")
            }
        }

        private val add_connectionTime_to_PeerAddress = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE PeerAddress ADD COLUMN connectionTime INTEGER")
            }
        }
    }
}
