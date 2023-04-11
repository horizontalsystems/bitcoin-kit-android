package io.horizontalsystems.bitcoincore.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_14_15 : Migration(14, 15) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE PublicKey ADD COLUMN convertedForP2TR BLOB DEFAULT '' NOT NULL")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_PublicKey_convertedForP2TR` ON `PublicKey` (`convertedForP2TR`)")
    }

}
