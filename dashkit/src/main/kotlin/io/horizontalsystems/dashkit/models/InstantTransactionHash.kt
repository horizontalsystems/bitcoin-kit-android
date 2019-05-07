package io.horizontalsystems.dashkit.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class InstantTransactionHash(@PrimaryKey val txHash: ByteArray)
