package io.horizontalsystems.bitcoincore.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
class SentTransaction() {

    @PrimaryKey
    var hashHexReversed: String = ""
    var firstSendTime: Long = System.currentTimeMillis()
    var lastSendTime: Long = System.currentTimeMillis()
    var retriesCount: Int = 0

    constructor(reversedHashHex: String) : this() {
        this.hashHexReversed = reversedHashHex
    }
}
