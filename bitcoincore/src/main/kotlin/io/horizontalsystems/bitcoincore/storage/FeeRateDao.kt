package io.horizontalsystems.bitcoincore.storage

import android.arch.persistence.room.*
import io.horizontalsystems.bitcoincore.models.FeeRate

@Dao
interface FeeRateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(rate: FeeRate)

    @Query("SELECT * FROM FeeRate limit 1")
    fun getRate(): FeeRate?

    @Delete
    fun delete(rate: FeeRate)
}
