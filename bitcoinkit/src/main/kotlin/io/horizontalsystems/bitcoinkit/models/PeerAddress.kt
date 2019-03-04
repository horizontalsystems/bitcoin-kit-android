package io.horizontalsystems.bitcoinkit.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class PeerAddress(@PrimaryKey var ip: String, var score: Int = 0)
