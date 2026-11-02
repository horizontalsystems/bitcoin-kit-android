package io.horizontalsystems.litecoinmwebkit.storage

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * An MWEB output received by the wallet. Output IDs identify outputs the way
 * transaction hashes identify Litecoin outputs; the daemon reports them hex
 * encoded.
 */
@Entity
data class MwebUtxo(
    @PrimaryKey val outputId: String,
    val value: Long,
    val address: String,
    val addressIndex: Int,
    /** Block height, 0 while unconfirmed. */
    val height: Int,
    val blockTime: Long,
    /** Set once the output has left the unspent set or was used by a send of this wallet. */
    val spent: Boolean = false,
    /** Uid of the wallet's own transaction that spent it, when known. */
    val spentByTxUid: String? = null,
    /** Uid of the wallet's own transaction that created it (change or peg-in), when known. */
    val createdByTxUid: String? = null,
)

/**
 * The wallet's transaction history. MWEB has no on-chain history to replay:
 * spent outputs vanish from the UTXO set, so rows are written when the event
 * happens and never rebuilt by a rescan.
 */
@Entity
data class MwebTransaction(
    @PrimaryKey val uid: String,
    val type: Int,
    /** Value received (incoming, peg-in) or sent to the recipient (outgoing, peg-out), in litoshi. */
    val amount: Long,
    /** Total fee paid by this wallet, when it created the transaction. */
    val fee: Long?,
    /** Block time, or 0 when unknown (confirmed below the header checkpoint). */
    val timestamp: Long,
    /** Block height, 0 while pending. */
    val height: Int,
    val status: Int,
    /** Recipient address for outgoing transactions. */
    val toAddress: String?,
    /** Own address an incoming output was received on. */
    val ownAddress: String?,
    /** Litecoin transaction hash of a peg-in or peg-out, hex, when known. */
    val txHash: String?,
    /** Comma separated output IDs spent by this transaction. */
    val inputIds: String,
    /** Comma separated output IDs created for this wallet by this transaction. */
    val outputIds: String,
) {
    companion object {
        const val TYPE_INCOMING = 0
        const val TYPE_OUTGOING = 1
        const val TYPE_PEG_IN = 2
        const val TYPE_PEG_OUT = 3
        /** Outputs left the unspent set without a send recorded here, e.g. by another wallet on the same seed. */
        const val TYPE_SPENT_ELSEWHERE = 4

        const val STATUS_PENDING = 0
        const val STATUS_CONFIRMED = 1
        const val STATUS_FAILED = 2
    }
}

/** Address pool entry; index 0 is reserved for change. */
@Entity
data class MwebAddressEntity(
    @PrimaryKey val index: Int,
    val address: String,
)

@Entity
data class MwebSyncInfo(
    @PrimaryKey val id: Int = 1,
    /** Height the UTXO stream has been consumed to; the next stream resumes a little below it. */
    val syncedHeight: Int,
)

@Dao
interface MwebUtxoDao {
    @Query("SELECT * FROM MwebUtxo")
    fun all(): List<MwebUtxo>

    @Query("SELECT * FROM MwebUtxo")
    fun allFlow(): Flow<List<MwebUtxo>>

    @Query("SELECT * FROM MwebUtxo WHERE spent = 0")
    fun unspent(): List<MwebUtxo>

    @Query("SELECT * FROM MwebUtxo WHERE outputId = :outputId")
    fun get(outputId: String): MwebUtxo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(utxo: MwebUtxo)

    @Update
    fun update(utxo: MwebUtxo)

    @Query("DELETE FROM MwebUtxo")
    fun deleteAll()
}

@Dao
interface MwebTransactionDao {
    @Query("SELECT * FROM MwebTransaction ORDER BY timestamp DESC, height DESC, uid DESC")
    fun allFlow(): Flow<List<MwebTransaction>>

    @Query("SELECT * FROM MwebTransaction ORDER BY timestamp DESC, height DESC, uid DESC")
    fun all(): List<MwebTransaction>

    @Query("SELECT * FROM MwebTransaction WHERE status = :status")
    fun withStatus(status: Int): List<MwebTransaction>

    @Query("SELECT * FROM MwebTransaction WHERE uid = :uid")
    fun get(uid: String): MwebTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: MwebTransaction)

    @Update
    fun update(transaction: MwebTransaction)
}

@Dao
interface MwebAddressDao {
    @Query("SELECT * FROM MwebAddressEntity ORDER BY `index`")
    fun all(): List<MwebAddressEntity>

    @Query("SELECT * FROM MwebAddressEntity WHERE address = :address")
    fun byAddress(address: String): MwebAddressEntity?

    @Query("SELECT * FROM MwebAddressEntity WHERE `index` = :index")
    fun byIndex(index: Int): MwebAddressEntity?

    @Query("SELECT MAX(`index`) FROM MwebAddressEntity")
    fun maxIndex(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(addresses: List<MwebAddressEntity>)
}

@Dao
interface MwebSyncInfoDao {
    @Query("SELECT * FROM MwebSyncInfo WHERE id = 1")
    fun get(): MwebSyncInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun set(info: MwebSyncInfo)
}

@Database(
    entities = [MwebUtxo::class, MwebTransaction::class, MwebAddressEntity::class, MwebSyncInfo::class],
    version = 1,
    exportSchema = false
)
abstract class MwebDatabase : RoomDatabase() {
    abstract fun utxos(): MwebUtxoDao
    abstract fun transactions(): MwebTransactionDao
    abstract fun addresses(): MwebAddressDao
    abstract fun syncInfo(): MwebSyncInfoDao

    companion object {
        fun open(context: Context, name: String): MwebDatabase =
            Room.databaseBuilder(context, MwebDatabase::class.java, name)
                .fallbackToDestructiveMigration()
                .build()
    }
}
