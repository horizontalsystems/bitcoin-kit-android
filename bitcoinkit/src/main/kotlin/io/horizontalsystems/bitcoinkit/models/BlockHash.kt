package io.horizontalsystems.bitcoinkit.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters
import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.storage.DataTypeConverters

@Entity
@TypeConverters(DataTypeConverters::class)
class BlockHash() {

    @PrimaryKey
    var reversedHeaderHashHex = ""

    var headerHash = byteArrayOf()
    var sequence: Int = 0
    var height: Int = 0

    constructor(headerHash: ByteArray, height: Int, sequence: Int = 0) : this() {
        this.headerHash = headerHash
        this.reversedHeaderHashHex = headerHash.reversedArray().toHexString()
        this.sequence = sequence
        this.height = height
    }

    constructor(reversedHeaderHashHex: String, height: Int) : this() {
        this.reversedHeaderHashHex = reversedHeaderHashHex
        this.headerHash = reversedHeaderHashHex.hexStringToByteArray().reversedArray()
        this.height = height
    }

}
