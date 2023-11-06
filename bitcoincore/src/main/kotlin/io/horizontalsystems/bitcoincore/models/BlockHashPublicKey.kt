package io.horizontalsystems.bitcoincore.models

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    primaryKeys = ["blockHash", "publicKeyPath"],
    foreignKeys = [
        ForeignKey(
            entity = BlockHash::class,
            parentColumns = ["headerHash"],
            childColumns = ["blockHash"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE,
            deferred = true
        ),
        ForeignKey(
            entity = PublicKey::class,
            parentColumns = ["path"],
            childColumns = ["publicKeyPath"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE,
            deferred = true
        )
    ]
)
class BlockHashPublicKey(
    val blockHash: ByteArray,
    val publicKeyPath: String
)
