package io.horizontalsystems.bitcoincore.storage.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bitcoincore.managers.PublicKeyManager

object Migration_18_19 : Migration(18, 19) {

    override fun migrate(database: SupportSQLiteDatabase) {
        try {
            database.beginTransaction()

            migratePublicKeyPath(database)
            migrateTransactionOutput(database)
            migrateBlockHashPublicKey(database)

            database.setTransactionSuccessful()
        } catch (error: Throwable) {
            Log.e("e", "error in migration", error)
        } finally {
            database.endTransaction()
        }
    }

    private fun migratePublicKeyPath(database: SupportSQLiteDatabase) {
        var cursor = database.query("SELECT * FROM `PublicKey`")
        val publicKeyPathIndex = cursor.getColumnIndex("path")

        if (publicKeyPathIndex >= 0) {

            while (cursor.moveToNext()) {
                val path = cursor.getString(publicKeyPathIndex)
                val fixedPath = fixedPath(path)

                database.update(
                    /* table = */ "PublicKey",
                    /* conflictAlgorithm = */ SQLiteDatabase.CONFLICT_IGNORE,
                    /* values = */ ContentValues().apply { put("path", "tmp-$fixedPath") },
                    /* whereClause = */ "path = ?",
                    /* whereArgs = */ arrayOf(path)
                )
            }

            cursor = database.query("SELECT * FROM `PublicKey`")

            while (cursor.moveToNext()) {
                val path = cursor.getString(publicKeyPathIndex)
                val fixedPath = path.removePrefix("tmp-")

                database.update(
                    /* table = */ "PublicKey",
                    /* conflictAlgorithm = */ SQLiteDatabase.CONFLICT_IGNORE,
                    /* values = */ ContentValues().apply { put("path", fixedPath) },
                    /* whereClause = */ "path = ?",
                    /* whereArgs = */ arrayOf(path)
                )
            }
        }
    }

    private fun migrateTransactionOutput(database: SupportSQLiteDatabase) {
        val cursor = database.query("SELECT * FROM `TransactionOutput` WHERE publicKeyPath IS NOT NULL")
        val publicKeyPathIndex = cursor.getColumnIndex("publicKeyPath")
        val transactionHashIndex = cursor.getColumnIndex("transactionHash")
        val indexIndex = cursor.getColumnIndex("index")

        if (publicKeyPathIndex >= 0 && transactionHashIndex >= 0 && indexIndex >= 0) {
            while (cursor.moveToNext()) {
                val transactionHash = cursor.getBlob(transactionHashIndex)
                val index = cursor.getString(indexIndex)
                val path = cursor.getString(publicKeyPathIndex)
                val fixedPath = fixedPath(path)

                database.update(
                    /* table = */ "TransactionOutput",
                    /* conflictAlgorithm = */ SQLiteDatabase.CONFLICT_IGNORE,
                    /* values = */ ContentValues().apply { put("publicKeyPath", fixedPath) },
                    /* whereClause = */ "transactionHash = ? AND `index` = ?",
                    /* whereArgs = */ arrayOf(transactionHash, index)
                )
            }
        }
    }

    private fun migrateBlockHashPublicKey(database: SupportSQLiteDatabase) {
        val cursor = database.query("SELECT * FROM `BlockHashPublicKey` WHERE publicKeyPath IS NOT NULL")
        val publicKeyPathIndex = cursor.getColumnIndex("publicKeyPath")
        val blockHashIndex = cursor.getColumnIndex("blockHash")

        if (publicKeyPathIndex >= 0 && blockHashIndex >= 0) {
            while (cursor.moveToNext()) {
                val blockHash = cursor.getBlob(blockHashIndex)
                val path = cursor.getString(publicKeyPathIndex)
                val fixedPath = fixedPath(path)

                database.update(
                    /* table = */ "BlockHashPublicKey",
                    /* conflictAlgorithm = */ SQLiteDatabase.CONFLICT_IGNORE,
                    /* values = */ ContentValues().apply {
                        put("publicKeyPath", fixedPath)
                    },
                    /* whereClause = */ "blockHash = ? AND publicKeyPath = ?",
                    /* whereArgs = */ arrayOf(blockHash, path)
                )
            }
        }
    }

    private fun fixedPath(path: String): String {
        val parts = path.split("/").map { it.toInt() }
        if (parts.size != 3) return path
        val account = parts[0]
        val change = if (parts[1] == 0) 1 else 0
        val index = parts[2]
        return "$account/$change/$index"
    }

}
