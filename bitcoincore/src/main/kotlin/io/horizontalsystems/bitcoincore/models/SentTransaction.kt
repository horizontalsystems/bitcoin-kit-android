package io.horizontalsystems.bitcoincore.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey

@Entity
class SentTransaction() {

    @PrimaryKey
    var hash = byteArrayOf()
    var firstSendTime: Long = System.currentTimeMillis()
    var lastSendTime: Long = System.currentTimeMillis()
    var retriesCount: Int = 0

    @Ignore
    constructor(hash: ByteArray) : this() {
        this.hash = hash
    }
}
