package io.horizontalsystems.bitcoincore.storage

import androidx.room.*
import io.horizontalsystems.bitcoincore.models.BlockHash

@Dao
interface BlockHashDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(blockHash: BlockHash)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(users: List<BlockHash>)

    @Query("SELECT headerHash FROM BlockHash")
    fun allBlockHashes(): List<ByteArray>

    @Query("SELECT headerHash FROM BlockHash WHERE headerHash NOT IN(:excludedHash)")
    fun allBlockHashes(excludedHash: List<ByteArray>): List<ByteArray>

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

    @Query("DELETE FROM BlockHash WHERE headerHash = :hash")
    fun delete(hash: ByteArray)
}
