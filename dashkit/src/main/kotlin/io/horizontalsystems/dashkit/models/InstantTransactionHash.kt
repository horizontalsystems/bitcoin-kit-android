package io.horizontalsystems.dashkit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class InstantTransactionHash(@PrimaryKey val txHash: ByteArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InstantTransactionHash) return false
        if (!txHash.contentEquals(other.txHash)) return false

        return true
    }

    override fun hashCode(): Int {
        return txHash.contentHashCode()
    }

}

