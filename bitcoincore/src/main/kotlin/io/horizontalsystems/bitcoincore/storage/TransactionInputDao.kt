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

    @Query("select * from TransactionInput where transactionHashReversedHex = :hashHex and previousOutputIndex = :index")
    fun getInputsOfOutput(hashHex: String, index: Int): List<TransactionInput>

    @Query("select * from TransactionInput where transactionHashReversedHex = :hashHex ")
    fun getTransactionInputs(hashHex: String): List<TransactionInput>

    @Query("""
        SELECT inputs.*, outputs.publicKeyPath, outputs.value
         FROM TransactionInput as inputs
         LEFT JOIN TransactionOutput AS outputs ON outputs.transactionHashReversedHex = inputs.previousOutputTxReversedHex AND outputs.`index` = inputs.previousOutputIndex
         WHERE inputs.transactionHashReversedHex IN(:txHashes)
    """)
    fun getInputsWithPrevouts(txHashes: List<String>): List<InputWithPreviousOutput>

}
