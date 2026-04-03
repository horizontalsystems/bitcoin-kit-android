package io.horizontalsystems.litecoinkit.mweb.storage

import androidx.room.*
import io.horizontalsystems.litecoinkit.mweb.storage.entities.MwebOutput
import io.horizontalsystems.litecoinkit.mweb.storage.entities.MwebSyncState
import io.horizontalsystems.litecoinkit.mweb.storage.entities.MwebWalletOutput

@Dao
interface MwebDao {

    // --- Raw outputs ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertOutputs(outputs: List<MwebOutput>)

    @Query("SELECT COUNT(*) FROM mweb_outputs")
    fun getOutputCount(): Long

    // --- Wallet outputs (owned) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWalletOutput(output: MwebWalletOutput)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWalletOutputs(outputs: List<MwebWalletOutput>)

    @Query("SELECT COALESCE(SUM(value), 0) FROM mweb_wallet_outputs WHERE isSpent = 0")
    fun getTotalBalance(): Long

    @Query("SELECT COUNT(*) FROM mweb_wallet_outputs WHERE isSpent = 0")
    fun getUnspentOutputCount(): Int

    // --- Sync state ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSyncState(state: MwebSyncState)

    @Query("SELECT * FROM mweb_sync_state WHERE id = 1")
    fun getSyncState(): MwebSyncState?
}