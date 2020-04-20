package io.horizontalsystems.bitcoincore.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class BlockHash(
        @PrimaryKey
        val headerHash: ByteArray,
        val height: Int,
        val sequence: Int = 0)
