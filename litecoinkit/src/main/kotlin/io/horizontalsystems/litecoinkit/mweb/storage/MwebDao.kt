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

    @Query("SELECT outputId FROM mweb_wallet_outputs WHERE isSpent = 0")
    fun getUnspentWalletOutputIds(): List<String>

    @Query("UPDATE mweb_wallet_outputs SET isSpent = 1 WHERE outputId IN (:outputIds)")
    fun markOutputsAsSpent(outputIds: List<String>)

    @Query("""
        SELECT w.outputId, w.value, w.derivationScalar,
               o.commitment, o.senderPubKey, o.receiverPubKey, o.features,
               o.maskedValue, o.maskedNonce, o.rangeProofBytes,
               o.leafIndex, o.blockHash
        FROM mweb_wallet_outputs w
        INNER JOIN mweb_outputs o ON w.outputId = o.outputId
        WHERE w.isSpent = 0
        ORDER BY w.value ASC
    """)
    fun getSpendableOutputs(): List<SpendableOutput>

    data class SpendableOutput(
        val outputId: String,
        val value: Long,
        val derivationScalar: ByteArray,
        val commitment: ByteArray,
        val senderPubKey: ByteArray,
        val receiverPubKey: ByteArray,
        val features: Byte,
        val maskedValue: ByteArray,
        val maskedNonce: ByteArray,
        val rangeProofBytes: ByteArray,
        val leafIndex: Long,
        val blockHash: String
    )

    // --- Sync state ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSyncState(state: MwebSyncState)

    @Query("SELECT * FROM mweb_sync_state WHERE id = 1")
    fun getSyncState(): MwebSyncState?
}