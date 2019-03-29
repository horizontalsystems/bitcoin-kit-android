package io.horizontalsystems.bitcoinkit.storage

import android.arch.persistence.room.TypeConverter
import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.extensions.hexToByteArray

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
