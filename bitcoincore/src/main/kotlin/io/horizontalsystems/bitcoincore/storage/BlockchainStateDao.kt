package io.horizontalsystems.bitcoincore.storage

import androidx.room.*
import io.horizontalsystems.bitcoincore.models.BlockchainState

@Dao
interface BlockchainStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(state: BlockchainState)

    @Query("SELECT * FROM BlockchainState LIMIT 1")
    fun getState(): BlockchainState?

    @Delete
    fun delete(state: BlockchainState)
}
