package io.horizontalsystems.bitcoincore.storage

import android.arch.persistence.room.*
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

@Dao
interface TransactionOutputDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(output: TransactionOutput)

    @Query("select * from transactionOutput where transactionHash in (:txHashes)")
    fun getTransactionsOutputs(txHashes: List<ByteArray>): List<TransactionOutput>

    @Delete
    fun delete(output: TransactionOutput)

    @Delete
    fun deleteAll(outputs: List<TransactionOutput>)

    @Query("""
        SELECT TransactionOutput.*, PublicKey.*, `Transaction`.*, Block.*
        FROM TransactionOutput
          INNER JOIN PublicKey ON TransactionOutput.publicKeyPath = PublicKey.path
          INNER JOIN `Transaction` ON TransactionOutput.transactionHash = `Transaction`.hash
          LEFT JOIN Block ON `Transaction`.blockHash = Block.headerHash
        WHERE TransactionOutput.scriptType != 0
          AND NOT EXISTS (SELECT previousOutputIndex FROM TransactionInput where TransactionInput.previousOutputTxHash = TransactionOutput.transactionHash and TransactionInput.previousOutputIndex = TransactionOutput.`index`)
    """)
    fun getUnspents(): List<UnspentOutput>

    @Query("select * from TransactionOutput where transactionHash = :previousOutputTxHash and `index` = :previousOutputIndex limit 1")
    fun getPreviousOutput(previousOutputTxHash: ByteArray, previousOutputIndex: Int): TransactionOutput?

    @Query("select * from TransactionOutput where transactionHash = :hash")
    fun getByHash(hash: ByteArray): List<TransactionOutput>

    @Query("select * from TransactionOutput where publicKeyPath = :path")
    fun getListByPath(path: String): List<TransactionOutput>

    @Query("SELECT * FROM TransactionOutput WHERE publicKeyPath IS NOT NULL")
    fun getMyOutputs(): List<TransactionOutput>

    @Query("""
        SELECT outputs.*
        FROM TransactionOutput AS outputs
        INNER JOIN PublicKey as publicKey ON outputs.publicKeyPath = publicKey.path
        LEFT JOIN (
          SELECT
            inputs.previousOutputIndex,
            inputs.previousOutputTxHash,
            inputs.transactionHash AS txHash,
            Block.*
          FROM TransactionInput AS inputs
          INNER JOIN `Transaction` AS transactions ON inputs.transactionHash = transactions.hash
          LEFT JOIN Block ON transactions.blockHash = Block.headerHash
        )
        AS input ON input.previousOutputTxHash = outputs.transactionHash AND input.previousOutputIndex = outputs.`index`
        WHERE outputs.scriptType IN(${ScriptType.P2WPKH}, ${ScriptType.P2PK}, ${ScriptType.P2WPKHSH})
        AND (height IS NULL OR height > :blockHeight)
    """)
    fun getOutputsForBloomFilter(blockHeight: Int): List<TransactionOutput>

}

