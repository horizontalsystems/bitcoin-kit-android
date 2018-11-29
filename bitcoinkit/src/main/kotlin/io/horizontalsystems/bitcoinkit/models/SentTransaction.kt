package io.horizontalsystems.bitcoinkit.models

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class SentTransaction() : RealmObject() {

    @PrimaryKey
    var hashHexReversed: String = ""
    var firstSendTime: Long = System.currentTimeMillis()
    var lastSendTime: Long = System.currentTimeMillis()
    var retriesCount: Int = 0

    constructor(reversedHashHex: String) : this() {
        this.hashHexReversed = reversedHashHex
    }
}
