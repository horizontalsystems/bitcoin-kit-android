package io.horizontalsystems.bitcoinkit.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.TypeConverters
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.storage.WitnessConverter

/**
 * Transaction input
 *
 *  Size        Field                Description
 *  ===         =====                ===========
 *  32 bytes    OutputHash           Double SHA-256 hash of the transaction containing the output to be used by this input
 *  4 bytes     OutputIndex          Index of the output within the transaction
 *  VarInt      InputScriptLength    Script length
 *  Variable    InputScript          Script
 *  4 bytes     InputSeqNumber       Input sequence number (irrelevant unless transaction LockTime is non-zero)
 */

@Entity(indices = [Index("transactionHashReversedHex")],
        primaryKeys = ["previousOutputTxReversedHex", "previousOutputIndex"],
        foreignKeys = [ForeignKey(
                entity = Transaction::class,
                parentColumns = ["hashHexReversed"],
                childColumns = ["transactionHashReversedHex"])])

class TransactionInput(
        val previousOutputTxReversedHex: String,
        val previousOutputIndex: Long,
        var sigScript: ByteArray = byteArrayOf(),
        val sequence: Long = 0xffffffff) {

    var transactionHashReversedHex: String = ""
    var keyHash: ByteArray? = null
    var address: String? = ""

    @TypeConverters(WitnessConverter::class)
    var witness: List<ByteArray> = listOf()

    fun transaction(storage: IStorage): Transaction? {
        return storage.getTransaction(transactionHashReversedHex)
    }

}
