package io.horizontalsystems.bitcoincore.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PeerAddress(@PrimaryKey var ip: String, var score: Int = 0, var connectionTime: Long? = null)
