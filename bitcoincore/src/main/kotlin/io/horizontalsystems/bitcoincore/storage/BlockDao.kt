package io.horizontalsystems.bitcoincore.storage

import androidx.room.*
import io.horizontalsystems.bitcoincore.models.Block

@Dao
interface BlockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(block: Block)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(block: Block)

    @Query("SELECT * FROM Block WHERE stale = :stale ORDER BY height DESC limit 1")
    fun getLast(stale: Boolean): Block?

    @Query("SELECT * FROM Block WHERE stale = :stale ORDER BY height ASC limit 1")
    fun getFirst(stale: Boolean): Block?

    @Query("SELECT * FROM Block ORDER BY height DESC limit 1")
    fun getLastBlock(): Block?

    @Query("SELECT * FROM Block WHERE headerHash IN (:hashes)")
    fun getBlocks(hashes: List<ByteArray>): List<Block>

    @Query("SELECT COUNT(headerHash) FROM Block WHERE headerHash IN (:hashes)")
    fun getBlocksCount(hashes: List<ByteArray>): Int

    @Query("SELECT * FROM Block WHERE height >= :heightGreaterOrEqualTo AND stale = :stale")
    fun getBlocks(heightGreaterOrEqualTo: Int, stale: Boolean): List<Block>

    @Query("SELECT * FROM Block WHERE stale = :stale")
    fun getBlocksByStale(stale: Boolean): List<Block>

    @Query("SELECT * FROM Block WHERE height > :heightGreaterThan ORDER BY height DESC LIMIT :limit")
    fun getBlocks(heightGreaterThan: Int, limit: Int): List<Block>

    @Query("SELECT * FROM Block WHERE height >= :fromHeight AND height <= :toHeight ORDER BY height ASC")
    fun getBlocksChunk(fromHeight: Int, toHeight: Int): List<Block>

    @Query("SELECT * FROM Block WHERE headerHash = :hash limit 1")
    fun getBlockByHash(hash: ByteArray): Block?

    @Query("SELECT * FROM Block WHERE height = :height limit 1")
    fun getBlockByHeight(height: Int): Block?

    @Query("SELECT * FROM Block WHERE height = :height ORDER by stale DESC limit 1")
    fun getBlockByHeightStalePrioritized(height: Int): Block?

    @Query("SELECT COUNT(headerHash) FROM Block")
    fun count(): Int

    @Query("DELETE FROM Block WHERE height < :toHeight AND hasTransactions = 0")
    fun deleteBlocksWithoutTransactions(toHeight: Int)

    @Query("UPDATE Block SET stale = 0")
    fun unstaleAllBlocks()

    @Delete
    fun delete(block: Block)

    @Delete
    fun deleteAll(blocks: List<Block>)

    @Query("SELECT block_timestamp FROM Block WHERE height >= :from AND height <= :to ORDER BY block_timestamp ASC")
    fun getTimestamps(from: Int, to: Int) : List<Long>

}
