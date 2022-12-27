package io.horizontalsystems.bitcoincore.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_13_14 : Migration(13, 14) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Block ADD COLUMN partial INTEGER DEFAULT 0 NOT NULL")
        database.execSQL("UPDATE Block SET partial = 1 WHERE headerHash IN (SELECT headerHash FROM BlockHash)")
    }

}