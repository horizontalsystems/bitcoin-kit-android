package io.horizontalsystems.bitcoinkit.storage

import android.arch.persistence.room.*
import io.horizontalsystems.bitcoinkit.models.BlockHash

@Dao
interface BlockHashDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(blockHash: BlockHash)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(users: List<BlockHash>)

    @Query("SELECT headerHash FROM BlockHash")
    fun allBlockHashes(): List<ByteArray>

    @Query("SELECT headerHashReversedHex FROM BlockHash WHERE headerHashReversedHex != :excludedHex")
    fun allBlockHashes(excludedHex: String): List<String>

    @Query("SELECT * FROM BlockHash ORDER BY sequence ASC, height ASC LIMIT :limit")
    fun getBlockHashesSortedSequenceHeight(limit: Int): List<BlockHash>

    @Query("SELECT * FROM BlockHash WHERE height = 0")
    fun getBlockchainBlockHashes(): List<BlockHash>

    @Query("SELECT * FROM BlockHash ORDER BY sequence DESC LIMIT 1")
    fun getLastBlockHash(): BlockHash?

    @Query("SELECT * FROM BlockHash WHERE height = 0 ORDER BY sequence DESC LIMIT 1")
    fun getLastBlockchainBlockHash(): BlockHash?

    @Delete
    fun delete(blockHash: BlockHash)

    @Query("DELETE FROM BlockHash WHERE height = :height")
    fun delete(height: Int)

    @Query("DELETE FROM BlockHash WHERE headerHashReversedHex = :hashHex")
    fun delete(hashHex: String)
}
