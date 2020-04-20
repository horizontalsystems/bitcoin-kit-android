package io.horizontalsystems.bitcoincore.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.TypeConverters
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
 *
 *  Note: In order to enable nLockTime and disable Replace-By-Fee, nSequenceNumber must be set to 0xfffffffe (BIP-125)
 *
 */

@Entity(primaryKeys = ["previousOutputTxHash", "previousOutputIndex"],
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
        var sequence: Long = 0xfffffffe) {

    var transactionHash = byteArrayOf()
    var keyHash: ByteArray? = null
    var address: String? = ""

    @TypeConverters(WitnessConverter::class)
    var witness: List<ByteArray> = listOf()
}
