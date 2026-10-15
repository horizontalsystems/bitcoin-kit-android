package io.horizontalsystems.bitcoincore.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Invalidating a transaction whose stored inputs had been replaced by a conflicting
// transaction used to save it under a rehashed, wrong hash and leave the original row in
// `Transaction`. Both rows share the uid, so this finishes the move the invalidation started.
object Migration_19_20 : Migration(19, 20) {

    private const val brokenInvalidUids = """
        SELECT uid FROM InvalidTransaction
        WHERE hash NOT IN (SELECT transactionHash FROM TransactionMetadata)
          AND uid IN (SELECT uid FROM `Transaction`)
    """

    private const val leftoverHashes = """
        SELECT hash FROM `Transaction` WHERE uid IN (SELECT uid FROM InvalidTransaction)
    """

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            UPDATE TransactionOutput SET failedToSpend = 1
            WHERE EXISTS (
                SELECT 1 FROM TransactionInput
                INNER JOIN `Transaction` ON `Transaction`.hash = TransactionInput.transactionHash
                WHERE TransactionInput.previousOutputTxHash = TransactionOutput.transactionHash
                  AND TransactionInput.previousOutputIndex = TransactionOutput.`index`
                  AND `Transaction`.uid IN ($brokenInvalidUids)
            )
            """
        )

        database.execSQL(
            """
            UPDATE InvalidTransaction
            SET hash = (SELECT hash FROM `Transaction` WHERE `Transaction`.uid = InvalidTransaction.uid)
            WHERE uid IN ($brokenInvalidUids)
            """
        )

        database.execSQL("DELETE FROM TransactionInput WHERE transactionHash IN ($leftoverHashes)")
        database.execSQL("DELETE FROM TransactionOutput WHERE transactionHash IN ($leftoverHashes)")
        database.execSQL("DELETE FROM `Transaction` WHERE uid IN (SELECT uid FROM InvalidTransaction)")
    }
}
