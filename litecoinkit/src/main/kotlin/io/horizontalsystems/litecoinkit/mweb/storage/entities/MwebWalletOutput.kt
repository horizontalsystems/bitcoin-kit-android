package io.horizontalsystems.litecoinkit.mweb.storage.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * MWEB output confirmed to belong to this wallet (detected via ECDH scan).
 */
@Entity(tableName = "mweb_wallet_outputs")
data class MwebWalletOutput(
    @PrimaryKey val outputId: String,  // FK to MwebOutput.outputId
    val value: Long,                   // satoshis (recovered from maskedValue)
    val isSpent: Boolean = false
)