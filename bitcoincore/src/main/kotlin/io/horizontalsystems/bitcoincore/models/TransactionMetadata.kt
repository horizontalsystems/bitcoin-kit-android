package io.horizontalsystems.bitcoincore.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity
class TransactionMetadata(
    @PrimaryKey
    val transactionHash: ByteArray
) {
    var amount: Long = 0
    var type: TransactionType = TransactionType.Incoming
    var fee: Long? = null
}

enum class TransactionType(val value: Int) {
    Incoming(1),
    Outgoing(2),
    SentToSelf(3);

    companion object {
        fun fromValue(value: Int) = values().find { it.value == value }
    }
}

enum class TransactionFilterType(val types: List<TransactionType>) {
    Incoming(listOf(TransactionType.Incoming, TransactionType.SentToSelf)),
    Outgoing(listOf(TransactionType.Outgoing, TransactionType.SentToSelf))
}

class TransactionTypeConverter {
    @TypeConverter
    fun fromInt(value: Int?): TransactionType? {
        return value?.let { TransactionType.fromValue(it) }
    }

    @TypeConverter
    fun transactionTypeToInt(transactionTypeToInt: TransactionType?): Int? {
        return transactionTypeToInt?.value
    }
}