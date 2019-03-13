package io.horizontalsystems.bitcoinkit.storage

import android.arch.persistence.room.TypeConverter
import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import io.horizontalsystems.bitcoinkit.core.toHexString

class DataTypeConverters {
    @TypeConverter
    fun byteArrayFromString(string: String): ByteArray {
        return string.hexStringToByteArray()
    }

    @TypeConverter
    fun byteArrayToString(byteArray: ByteArray): String {
        return byteArray.toHexString()
    }
}
