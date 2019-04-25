package io.horizontalsystems.bitcoincore.storage

import android.arch.persistence.room.*
import io.horizontalsystems.bitcoincore.models.TransactionInput

@Dao
interface TransactionInputDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(input: TransactionInput)

    @Delete
    fun delete(input: TransactionInput)

    @Delete
    fun deleteAll(inputs: List<TransactionInput>)

    @Query("select * from TransactionInput where transactionHash = :hash and previousOutputIndex = :index")
    fun getInputsOfOutput(hash: ByteArray, index: Int): List<TransactionInput>

    @Query("select * from TransactionInput where transactionHash = :hash")
    fun getTransactionInputs(hash: ByteArray): List<TransactionInput>

    @Query("""
        SELECT inputs.*, outputs.publicKeyPath, outputs.value
         FROM TransactionInput as inputs
         LEFT JOIN TransactionOutput AS outputs ON outputs.transactionHash = inputs.previousOutputTxHash AND outputs.`index` = inputs.previousOutputIndex
         WHERE inputs.transactionHash IN(:txHashes)
    """)
    fun getInputsWithPrevouts(txHashes: List<ByteArray>): List<InputWithPreviousOutput>

}
