package io.horizontalsystems.bitcoinkit.storage

import android.arch.persistence.room.*
import io.horizontalsystems.bitcoinkit.models.TransactionOutput
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType

@Dao
interface TransactionOutputDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(output: TransactionOutput)

    @Query("SELECT * FROM TransactionOutput WHERE transactionHashReversedHex = :hashHex")
    fun getListByTransactionHash(hashHex: String): List<TransactionOutput>

    @Query("select * from transactionOutput where transactionHashReversedHex in (:txHashes)")
    fun getTransactionsOutputs(txHashes: List<String>): List<TransactionOutput>

    @Delete
    fun delete(output: TransactionOutput)

    @Delete
    fun deleteAll(outputs: List<TransactionOutput>)

    @Query("""
        SELECT TransactionOutput.*, PublicKey.*, `Transaction`.*, Block.*
        FROM TransactionOutput
          INNER JOIN PublicKey ON TransactionOutput.publicKeyPath = PublicKey.path
          INNER JOIN `Transaction` ON TransactionOutput.transactionHashReversedHex = `Transaction`.hashHexReversed
          LEFT JOIN Block ON `Transaction`.blockHashReversedHex = Block.headerHashReversedHex
        WHERE TransactionOutput.scriptType != 0
          AND NOT EXISTS (SELECT previousOutputIndex FROM TransactionInput where TransactionInput.previousOutputTxReversedHex = TransactionOutput.transactionHashReversedHex and TransactionInput.previousOutputIndex = TransactionOutput.`index`)
    """)
    fun getUnspents(): List<UnspentOutput>

    @Query("select * from TransactionOutput where transactionHashReversedHex = :previousOutputTxReversedHex and `index` = :previousOutputIndex limit 1")
    fun getPreviousOutput(previousOutputTxReversedHex: String, previousOutputIndex: Int): TransactionOutput?

    @Query("select * from TransactionOutput where transactionHashReversedHex = :hashHex")
    fun getByHashHex(hashHex: String): List<TransactionOutput>

    @Query("select * from TransactionOutput where publicKeyPath = :path")
    fun getListByPath(path: String): List<TransactionOutput>

    @Query("""
        SELECT outputs.*
        FROM TransactionOutput AS outputs
        INNER JOIN PublicKey as publicKey ON outputs.publicKeyPath = publicKey.path
        LEFT JOIN (
          SELECT
            inputs.previousOutputIndex,
            inputs.previousOutputTxReversedHex,
            inputs.transactionHashReversedHex AS txReversedHex,
            Block.*
          FROM TransactionInput AS inputs
          INNER JOIN `Transaction` AS transactions ON inputs.transactionHashReversedHex = transactions.hashHexReversed
          LEFT JOIN Block ON transactions.blockHashReversedHex = Block.headerHashReversedHex
        )
        AS input ON input.txReversedHex = outputs.transactionHashReversedHex AND input.previousOutputIndex = outputs.`index`
        WHERE outputs.scriptType = ${ScriptType.P2WPKH} OR outputs.scriptType = ${ScriptType.P2PK}
    """)
    fun getMyOutputs(): List<FullOutputInfo>

}

