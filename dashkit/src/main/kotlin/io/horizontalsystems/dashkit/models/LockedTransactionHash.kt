package io.horizontalsystems.dashkit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class LockedTransactionHash(@PrimaryKey val txHash: ByteArray)