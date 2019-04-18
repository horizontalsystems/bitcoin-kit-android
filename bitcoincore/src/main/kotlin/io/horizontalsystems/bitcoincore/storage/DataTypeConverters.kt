package io.horizontalsystems.bitcoincore.storage

import android.arch.persistence.room.TypeConverter
import io.horizontalsystems.bitcoincore.core.toHexString
import io.horizontalsystems.bitcoincore.extensions.hexToByteArray

class WitnessConverter {

    @TypeConverter
    fun fromWitness(list: List<ByteArray>): String {
        return list.joinToString(", ") {
            it.toHexString()
        }
    }

    @TypeConverter
    fun toWitness(data: String): List<ByteArray> {
        return data.split(", ").map {
            it.hexToByteArray()
        }
    }
}
