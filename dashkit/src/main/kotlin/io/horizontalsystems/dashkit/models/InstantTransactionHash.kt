package io.horizontalsystems.dashkit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class InstantTransactionHash(@PrimaryKey val txHash: ByteArray) {

    override fun equals(other: Any?): Boolean {
        return other === this || other is InstantTransactionHash && txHash.contentEquals(other.txHash)
    }

    override fun hashCode(): Int {
        return txHash.contentHashCode()
    }

}

