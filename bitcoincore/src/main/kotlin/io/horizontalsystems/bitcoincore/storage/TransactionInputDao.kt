package io.horizontalsystems.bitcoincore.storage

import androidx.room.*
import io.horizontalsystems.bitcoincore.models.TransactionInput

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

    @Query("""
        SELECT 
            inputs.*, 
            outputs.publicKeyPath, 
            outputs.value, 
            outputs.`index`, 
            outputs.transactionHash as outputTransactionHash
         FROM TransactionInput as inputs
         LEFT JOIN TransactionOutput AS outputs ON outputs.transactionHash = inputs.previousOutputTxHash AND outputs.`index` = inputs.previousOutputIndex
         WHERE inputs.transactionHash IN(:txHashes)
    """)
    fun getInputsWithPrevouts(txHashes: List<ByteArray>): List<InputWithPreviousOutput>

    @Query("SELECT * FROM TransactionInput WHERE previousOutputTxHash = :txHash")
    fun getInputsByPrevOutputTxHash(txHash: ByteArray): List<TransactionInput>

    @Query("SELECT * FROM TransactionInput where TransactionInput.previousOutputTxHash = :prevOutputTxHash and TransactionInput.previousOutputIndex = :prevOutputIndex")
    fun getInput(prevOutputTxHash: ByteArray, prevOutputIndex: Long): TransactionInput?

}
