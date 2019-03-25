package io.horizontalsystems.bitcoinkit.storage

import android.arch.persistence.room.*
import io.horizontalsystems.bitcoinkit.models.TransactionInput

@Dao
interface TransactionInputDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(input: TransactionInput)

    @Delete
    fun delete(input: TransactionInput)

    @Delete
    fun deleteAll(inputs: List<TransactionInput>)

    @Query("select * from TransactionInput where transactionHashReversedHex = :hashHex and previousOutputIndex = :index")
    fun getInputsOfOutput(hashHex: String, index: Int): List<TransactionInput>

    @Query("select * from TransactionInput where transactionHashReversedHex = :hashHex ")
    fun getTransactionInputs(hashHex: String): List<TransactionInput>

    @Query("SELECT TransactionInput.*, Block.* FROM TransactionInput INNER JOIN `Transaction` as transactions ON TransactionInput.transactionHashReversedHex = transactions.hashHexReversed LEFT JOIN  Block ON transactions.blockHashReversedHex = Block.headerHashReversedHex WHERE TransactionInput.previousOutputTxReversedHex = :hashHex AND TransactionInput.previousOutputIndex = :index")
    fun getInputsWithBlock(hashHex: String, index: Int): List<InputWithBlock>

}
