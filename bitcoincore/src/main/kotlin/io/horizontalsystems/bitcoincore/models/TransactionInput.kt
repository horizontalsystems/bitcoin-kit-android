package io.horizontalsystems.bitcoincore.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.TypeConverters
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.storage.WitnessConverter

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

@Entity(indices = [Index("transactionHash")],
        primaryKeys = ["previousOutputTxHash", "previousOutputIndex"],
        foreignKeys = [ForeignKey(
                entity = Transaction::class,
                parentColumns = ["hash"],
                childColumns = ["transactionHash"],
                onUpdate = ForeignKey.CASCADE,
                onDelete = ForeignKey.CASCADE,
                deferred = true)
        ])

class TransactionInput(
        val previousOutputTxHash: ByteArray,
        val previousOutputIndex: Long,
        var sigScript: ByteArray = byteArrayOf(),
        val sequence: Long = 0xffffffff) {

    var transactionHash = byteArrayOf()
    var keyHash: ByteArray? = null
    var address: String? = ""

    @TypeConverters(WitnessConverter::class)
    var witness: List<ByteArray> = listOf()

    fun transaction(storage: IStorage): Transaction? {
        return storage.getTransaction(transactionHash)
    }

}
