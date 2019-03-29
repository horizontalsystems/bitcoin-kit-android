package io.horizontalsystems.bitcoinkit.storage

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import io.horizontalsystems.bitcoinkit.models.*

@Database(version = 1, exportSchema = false, entities = [
    FeeRate::class,
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

abstract class KitDatabase : RoomDatabase() {

    abstract val feeRate: FeeRateDao
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
