package io.horizontalsystems.bitcoincore.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class SentTransaction() {

    @PrimaryKey
    var hash = byteArrayOf()
    var firstSendTime: Long = System.currentTimeMillis()
    var lastSendTime: Long = System.currentTimeMillis()
    var retriesCount: Int = 0
    var sendSuccess: Boolean = false

    constructor(hash: ByteArray) : this() {
        this.hash = hash
    }
}
