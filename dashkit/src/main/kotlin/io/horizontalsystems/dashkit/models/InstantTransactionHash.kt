package io.horizontalsystems.dashkit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class InstantTransactionHash(@PrimaryKey val txHash: ByteArray)
