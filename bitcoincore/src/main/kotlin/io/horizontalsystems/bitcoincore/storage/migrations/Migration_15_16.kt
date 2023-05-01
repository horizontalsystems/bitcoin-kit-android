package io.horizontalsystems.bitcoincore.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_15_16 : Migration(15, 16) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE `TransactionOutput` SET `lockingScriptPayload` = SUBSTR(`lockingScriptPayload`, 3) WHERE scriptType = 4 and publicKeyPath is not null")
    }

}
