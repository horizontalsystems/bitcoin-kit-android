package io.horizontalsystems.bitcoinkit.storage

import android.arch.persistence.room.*
import io.horizontalsystems.bitcoinkit.models.Transaction

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: Transaction)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(transaction: Transaction)

    @Query("select * from `Transaction` where hashHexReversed = :hashHex")
    fun getByHashHex(hashHex: String): Transaction?

    @Query("select * from `Transaction` where hash = :hash")
    fun getByHash(hash: ByteArray): Transaction?

    @Query("select * from `Transaction` where blockHashReversedHex = :blockHashHex")
    fun getBlockTransactions(blockHashHex: String): List<Transaction>

    @Query("select * from `Transaction` where hashHexReversed = :hashHex and status = 1 limit 1")
    fun getNewTransaction(hashHex: String): Transaction?

    @Query("select * from `Transaction` where status = 1")
    fun getNewTransactions(): List<Transaction>

    @Query("select * from `transaction` order by timestamp DESC, `order` DESC")
    fun getSortedTimestampAndOrdered(): List<Transaction>

    @Delete
    fun delete(transaction: Transaction)

    @Delete
    fun deleteAll(transactions: List<Transaction>)


}
