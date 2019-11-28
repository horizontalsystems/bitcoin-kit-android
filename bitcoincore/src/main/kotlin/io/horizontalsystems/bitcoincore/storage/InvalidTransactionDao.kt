package io.horizontalsystems.bitcoincore.storage

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.horizontalsystems.bitcoincore.models.InvalidTransaction

@Dao
interface InvalidTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: InvalidTransaction)

    @Query("DELETE FROM InvalidTransaction")
    fun deleteAll()

}
