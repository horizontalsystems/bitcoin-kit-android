package io.horizontalsystems.bitcoinkit.storage

import android.arch.persistence.room.*
import io.horizontalsystems.bitcoinkit.models.Block

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

    @Query("SELECT * FROM Block WHERE headerHashReversedHex IN (:hashHex)")
    fun getBlocks(hashHex: List<String>): List<Block>

    @Query("SELECT COUNT(headerHashReversedHex) FROM Block WHERE headerHashReversedHex IN (:hashHex)")
    fun getBlocksCount(hashHex: List<String>): Int

    @Query("SELECT * FROM Block WHERE height >= :heightGreaterOrEqualTo AND stale = :stale")
    fun getBlocks(heightGreaterOrEqualTo: Int, stale: Boolean): List<Block>

    @Query("SELECT * FROM Block WHERE stale = :stale")
    fun getBlocksByStale(stale: Boolean): List<Block>

    @Query("SELECT * FROM Block WHERE height > :heightGreaterThan ORDER BY height DESC LIMIT :limit")
    fun getBlocks(heightGreaterThan: Int, limit: Int): List<Block>

    @Query("SELECT * FROM Block WHERE height <= :fromHeight AND height > :toHeight ORDER BY height ASC")
    fun getBlocksChunk(fromHeight: Int, toHeight: Int): List<Block>

    @Query("SELECT * FROM Block WHERE headerHashReversedHex = :hashHex limit 1")
    fun getBlockByHex(hashHex: String): Block?

    @Query("SELECT * FROM Block WHERE height = :height limit 1")
    fun getBlockByHeight(height: Int): Block?

    @Query("SELECT COUNT(headerHashReversedHex) FROM Block")
    fun count(): Int

    @Delete
    fun delete(block: Block)

    @Delete
    fun deleteAll(blocks: List<Block>)

}
