package io.horizontalsystems.bitcoincore.storage

import android.arch.persistence.db.SupportSQLiteQuery
import android.arch.persistence.room.*
import io.horizontalsystems.bitcoincore.models.Transaction

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: Transaction)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(transaction: Transaction)

    @Query("select * from `Transaction` where hash = :hash")
    fun getByHash(hash: ByteArray): Transaction?

    @Query("select * from `Transaction` where blockHash = :blockHash")
    fun getBlockTransactions(blockHash: ByteArray): List<Transaction>

    @Query("select * from `Transaction` where hash = :hash and status = 1 limit 1")
    fun getNewTransaction(hash: ByteArray): Transaction?

    @Query("select * from `Transaction` where status = 1")
    fun getNewTransactions(): List<Transaction>

    @RawQuery
    fun getTransactionWithBlockBySql(query: SupportSQLiteQuery): List<TransactionWithBlock>

    @Delete
    fun delete(transaction: Transaction)

    @Delete
    fun deleteAll(transactions: List<Transaction>)

}
