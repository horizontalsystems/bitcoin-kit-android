package io.horizontalsystems.bitcoincore.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_16_17 : Migration(16, 17) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `BlockHashPublicKey` (`blockHash` BLOB NOT NULL, `publicKeyPath` TEXT NOT NULL, PRIMARY KEY(`blockHash`, `publicKeyPath`), FOREIGN KEY(`blockHash`) REFERENCES `BlockHash`(`headerHash`) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED, FOREIGN KEY(`publicKeyPath`) REFERENCES `PublicKey`(`path`) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)")
    }

}
