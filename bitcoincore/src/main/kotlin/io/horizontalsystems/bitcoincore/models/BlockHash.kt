package io.horizontalsystems.bitcoincore.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
class BlockHash(
        @PrimaryKey
        val headerHash: ByteArray,
        val height: Int,
        val sequence: Int = 0)
