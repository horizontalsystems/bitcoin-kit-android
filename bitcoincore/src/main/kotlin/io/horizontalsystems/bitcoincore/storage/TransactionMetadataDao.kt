package io.horizontalsystems.bitcoincore.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bitcoincore.models.TransactionMetadata

@Dao
interface TransactionMetadataDao {

    @Query("SELECT * FROM `TransactionMetadata` WHERE `transactionHash` IN (:txHashes)")
    fun getTransactionMetadata(txHashes: List<ByteArray>): List<TransactionMetadata>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(metadata: TransactionMetadata)

}
