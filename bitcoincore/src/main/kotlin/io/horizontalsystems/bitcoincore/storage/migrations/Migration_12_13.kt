package io.horizontalsystems.bitcoincore.storage.migrations

import android.database.Cursor
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.WitnessConverter
import io.horizontalsystems.bitcoincore.transactions.extractors.ITransactionOutputProvider
import io.horizontalsystems.bitcoincore.transactions.extractors.MyOutputsCache
import io.horizontalsystems.bitcoincore.transactions.extractors.TransactionMetadataExtractor
import java.util.ArrayList

object Migration_12_13 : Migration(12, 13) {

    private val __witnessConverter = WitnessConverter()
    private val __scriptTypeConverter = ScriptTypeConverter()

    override fun migrate(database: SupportSQLiteDatabase) {
        createTableTransactionMetadata(database)
        createMetadataForExistingTransactions(database)
        deleteInvalidTransactions(database)
    }

    private fun deleteInvalidTransactions(database: SupportSQLiteDatabase) {
        database.execSQL("DELETE FROM `InvalidTransaction`")
    }

    private fun createTableTransactionMetadata(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `TransactionMetadata` (`amount` INTEGER NOT NULL, `type` INTEGER NOT NULL, `fee` INTEGER, `transactionHash` BLOB NOT NULL, PRIMARY KEY(`transactionHash`))")
    }

    private fun createMetadataForExistingTransactions(database: SupportSQLiteDatabase) {
        val transactions = getTransactions(database)
        val inputs = getTransactionInputs(database)
        val outputs = getTransactionOutputs(database)

        val myOutputsCache = MyOutputsCache().apply {
            add(outputs)
        }

        val outputProvider = object : ITransactionOutputProvider {
            override fun get(transactionHash: ByteArray, index: Int): TransactionOutput? {
                return outputs.find {
                    it.transactionHash.contentEquals(transactionHash) && it.index == index
                }
            }
        }

        val metadataExtractor = TransactionMetadataExtractor(myOutputsCache, outputProvider)
        transactions.forEach { transaction ->
            val transactionInputs = inputs.filter {
                it.transactionHash.contentEquals(transaction.hash)
            }

            val transactionOutputs = outputs.filter {
                it.transactionHash.contentEquals(transaction.hash)
            }

            val fullTransaction =
                FullTransaction(transaction, transactionInputs, transactionOutputs)
            metadataExtractor.extract(fullTransaction)

            insertTransactionMetadata(database, fullTransaction.metadata)
        }
    }

    private fun insertTransactionMetadata(database: SupportSQLiteDatabase, metadata: TransactionMetadata) {
        database.execSQL(
            "INSERT OR REPLACE INTO `TransactionMetadata` (transactionHash,amount,type,fee) VALUES(?, ?, ?, ?)",
            arrayOf(metadata.transactionHash, metadata.amount, metadata.type.value, metadata.fee)
        )
    }

    private fun getTransactionOutputs(database: SupportSQLiteDatabase): List<TransactionOutput> {
        val _cursor = database.query("SELECT * FROM TransactionOutput ORDER BY rowId")
        return try {
            val _cursorIndexOfValue = getColumnIndexOrThrow(_cursor, "value")
            val _cursorIndexOfLockingScript =
                getColumnIndexOrThrow(_cursor, "lockingScript")
            val _cursorIndexOfRedeemScript =
                getColumnIndexOrThrow(_cursor, "redeemScript")
            val _cursorIndexOfIndex = getColumnIndexOrThrow(_cursor, "index")
            val _cursorIndexOfTransactionHash =
                getColumnIndexOrThrow(_cursor, "transactionHash")
            val _cursorIndexOfPublicKeyPath =
                getColumnIndexOrThrow(_cursor, "publicKeyPath")
            val _cursorIndexOfChangeOutput =
                getColumnIndexOrThrow(_cursor, "changeOutput")
            val _cursorIndexOfScriptType = getColumnIndexOrThrow(_cursor, "scriptType")
            val _cursorIndexOfKeyHash = getColumnIndexOrThrow(_cursor, "keyHash")
            val _cursorIndexOfAddress = getColumnIndexOrThrow(_cursor, "address")
            val _cursorIndexOfFailedToSpend =
                getColumnIndexOrThrow(_cursor, "failedToSpend")
            val _cursorIndexOfPluginId = getColumnIndexOrThrow(_cursor, "pluginId")
            val _cursorIndexOfPluginData = getColumnIndexOrThrow(_cursor, "pluginData")
            val _result: MutableList<TransactionOutput> = ArrayList(_cursor.count)
            while (_cursor.moveToNext()) {
                val _item: TransactionOutput
                _item = TransactionOutput()
                _item.value = _cursor.getLong(_cursorIndexOfValue)
                _item.lockingScript = _cursor.getBlob(_cursorIndexOfLockingScript)
                _item.redeemScript = _cursor.getBlob(_cursorIndexOfRedeemScript)
                _item.index = _cursor.getInt(_cursorIndexOfIndex)
                _item.transactionHash = _cursor.getBlob(_cursorIndexOfTransactionHash)
                _item.publicKeyPath = _cursor.getString(_cursorIndexOfPublicKeyPath)
                _item.changeOutput = _cursor.getInt(_cursorIndexOfChangeOutput) != 0
                val _tmp_1 = if (_cursor.isNull(_cursorIndexOfScriptType)) {
                    null
                } else {
                    _cursor.getInt(_cursorIndexOfScriptType)
                }
                __scriptTypeConverter.fromInt(_tmp_1)?.let {
                    _item.scriptType = it
                }
                _item.keyHash = _cursor.getBlob(_cursorIndexOfKeyHash)
                _item.address = _cursor.getString(_cursorIndexOfAddress)
                _item.failedToSpend = _cursor.getInt(_cursorIndexOfFailedToSpend) != 0
                _item.pluginId = if (_cursor.isNull(_cursorIndexOfPluginId)) {
                    null
                } else {
                    _cursor.getShort(_cursorIndexOfPluginId).toByte()
                }
                _item.pluginData = _cursor.getString(_cursorIndexOfPluginData)
                _result.add(_item)
            }
            _result
        } finally {
            _cursor.close()
        }
    }

    private fun getTransactionInputs(database: SupportSQLiteDatabase): List<TransactionInput> {
        val _cursor = database.query("SELECT * FROM TransactionInput")
        return try {
            val _cursorIndexOfTransactionHash =
                getColumnIndexOrThrow(_cursor, "transactionHash")
            val _cursorIndexOfKeyHash = getColumnIndexOrThrow(_cursor, "keyHash")
            val _cursorIndexOfAddress = getColumnIndexOrThrow(_cursor, "address")
            val _cursorIndexOfWitness = getColumnIndexOrThrow(_cursor, "witness")
            val _cursorIndexOfPreviousOutputTxHash =
                getColumnIndexOrThrow(_cursor, "previousOutputTxHash")
            val _cursorIndexOfPreviousOutputIndex =
                getColumnIndexOrThrow(_cursor, "previousOutputIndex")
            val _cursorIndexOfSigScript = getColumnIndexOrThrow(_cursor, "sigScript")
            val _cursorIndexOfSequence = getColumnIndexOrThrow(_cursor, "sequence")
            val _result: MutableList<TransactionInput> = ArrayList(_cursor.count)
            while (_cursor.moveToNext()) {
                val _item: TransactionInput
                _item = TransactionInput(
                    _cursor.getBlob(_cursorIndexOfPreviousOutputTxHash),
                    _cursor.getLong(_cursorIndexOfPreviousOutputIndex),
                    _cursor.getBlob(_cursorIndexOfSigScript),
                    _cursor.getLong(_cursorIndexOfSequence)
                )
                _item.transactionHash = _cursor.getBlob(_cursorIndexOfTransactionHash)
                _item.keyHash = _cursor.getBlob(_cursorIndexOfKeyHash)
                _item.address = _cursor.getString(_cursorIndexOfAddress)
                _item.witness = __witnessConverter.toWitness(_cursor.getString(_cursorIndexOfWitness))
                _result.add(_item)
            }
            _result
        } finally {
            _cursor.close()
        }
    }

    private fun getTransactions(database: SupportSQLiteDatabase): List<Transaction> {
        val _cursor = database.query("SELECT * FROM `Transaction`")
        return try {
            val _cursorIndexOfUid = getColumnIndexOrThrow(_cursor, "uid")
            val _cursorIndexOfHash = getColumnIndexOrThrow(_cursor, "hash")
            val _cursorIndexOfBlockHash = getColumnIndexOrThrow(_cursor, "blockHash")
            val _cursorIndexOfVersion = getColumnIndexOrThrow(_cursor, "version")
            val _cursorIndexOfLockTime = getColumnIndexOrThrow(_cursor, "lockTime")
            val _cursorIndexOfTimestamp = getColumnIndexOrThrow(_cursor, "timestamp")
            val _cursorIndexOfOrder = getColumnIndexOrThrow(_cursor, "order")
            val _cursorIndexOfIsMine = getColumnIndexOrThrow(_cursor, "isMine")
            val _cursorIndexOfIsOutgoing = getColumnIndexOrThrow(_cursor, "isOutgoing")
            val _cursorIndexOfSegwit = getColumnIndexOrThrow(_cursor, "segwit")
            val _cursorIndexOfStatus = getColumnIndexOrThrow(_cursor, "status")
            val _cursorIndexOfSerializedTxInfo =
                getColumnIndexOrThrow(_cursor, "serializedTxInfo")
            val _cursorIndexOfConflictingTxHash =
                getColumnIndexOrThrow(_cursor, "conflictingTxHash")
            val _cursorIndexOfRawTransaction =
                getColumnIndexOrThrow(_cursor, "rawTransaction")
            val _result: MutableList<Transaction> = ArrayList(_cursor.count)
            while (_cursor.moveToNext()) {
                val _item: Transaction
                _item = Transaction()
                _item.uid = _cursor.getString(_cursorIndexOfUid)
                _item.hash = _cursor.getBlob(_cursorIndexOfHash)
                _item.blockHash = _cursor.getBlob(_cursorIndexOfBlockHash)
                _item.version = _cursor.getInt(_cursorIndexOfVersion)
                _item.lockTime = _cursor.getLong(_cursorIndexOfLockTime)
                _item.timestamp = _cursor.getLong(_cursorIndexOfTimestamp)
                _item.order = _cursor.getInt(_cursorIndexOfOrder)
                _item.isMine = _cursor.getInt(_cursorIndexOfIsMine) != 0
                _item.isOutgoing = _cursor.getInt(_cursorIndexOfIsOutgoing) != 0
                _item.segwit = _cursor.getInt(_cursorIndexOfSegwit) != 0
                _item.status = _cursor.getInt(_cursorIndexOfStatus)
                _item.serializedTxInfo = _cursor.getString(_cursorIndexOfSerializedTxInfo)
                _item.conflictingTxHash = _cursor.getBlob(_cursorIndexOfConflictingTxHash)
                _item.rawTransaction = _cursor.getString(_cursorIndexOfRawTransaction)
                _result.add(_item)
            }
            _result
        } finally {
            _cursor.close()
        }
    }

    private fun getColumnIndexOrThrow(c: Cursor, name: String): Int {
        val index = c.getColumnIndex(name)
        return when {
            index >= 0 -> index
            else -> c.getColumnIndexOrThrow("`$name`")
        }
    }


}