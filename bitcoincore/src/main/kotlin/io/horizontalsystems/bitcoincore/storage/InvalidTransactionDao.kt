package io.horizontalsystems.bitcoincore.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bitcoincore.models.InvalidTransaction

@Dao
interface InvalidTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: InvalidTransaction)

    @Query("DELETE FROM InvalidTransaction")
    fun deleteAll()

    @Query("DELETE FROM InvalidTransaction where uid = :uid")
    fun delete(uid: String)

}
