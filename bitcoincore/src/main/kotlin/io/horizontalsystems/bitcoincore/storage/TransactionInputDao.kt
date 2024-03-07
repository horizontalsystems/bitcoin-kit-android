package io.horizontalsystems.bitcoincore.storage

import androidx.room.*
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput

@Dao
interface TransactionInputDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(input: TransactionInput)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(input: TransactionInput)

    @Delete
    fun delete(input: TransactionInput)

    @Delete
    fun deleteAll(inputs: List<TransactionInput>)

    @Query("DELETE FROM TransactionInput WHERE transactionHash = :txHash")
    fun deleteByTxHash(txHash: ByteArray)

    @Query("select * from TransactionInput where transactionHash = :hash order by rowId")
    fun getTransactionInputs(hash: ByteArray): List<TransactionInput>

    @Query("select * from TransactionInput where transactionHash IN (:hashes)")
    fun getTransactionInputs(hashes: List<ByteArray>): List<TransactionInput>

    @Query("select * from TransactionOutput where transactionHash=:transactionHash AND `index`=:index limit 1")
    fun output(transactionHash: ByteArray, index: Long): TransactionOutput?

    @Transaction
    fun getInputsWithPrevouts(txHashes: List<ByteArray>) =
        getTransactionInputs(txHashes).map { input ->
            val prevOutput = output(input.previousOutputTxHash, input.previousOutputIndex)
            InputWithPreviousOutput(input, prevOutput)
        }

    @Query("SELECT * FROM TransactionInput WHERE previousOutputTxHash = :txHash")
    fun getInputsByPrevOutputTxHash(txHash: ByteArray): List<TransactionInput>

    @Query("SELECT * FROM TransactionInput where TransactionInput.previousOutputTxHash = :prevOutputTxHash and TransactionInput.previousOutputIndex = :prevOutputIndex")
    fun getInput(prevOutputTxHash: ByteArray, prevOutputIndex: Long): TransactionInput?

}
