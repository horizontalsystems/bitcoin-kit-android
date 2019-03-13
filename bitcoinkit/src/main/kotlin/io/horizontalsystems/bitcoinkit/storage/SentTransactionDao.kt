package io.horizontalsystems.bitcoinkit.storage

import android.arch.persistence.room.*
import io.horizontalsystems.bitcoinkit.models.SentTransaction

@Dao
interface SentTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: SentTransaction)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(transaction: SentTransaction)

    @Query("select * from SentTransaction where hashHexReversed = :hashHex limit 1")
    fun getTransaction(hashHex: String): SentTransaction?

    @Delete
    fun delete(transaction: SentTransaction)
}
