package io.horizontalsystems.bitcoinkit.models

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class PeerAddress() : RealmObject() {

    @PrimaryKey
    var ip: String = ""
    var score: Int = 0

    constructor(peerIp: String) : this() {
        ip = peerIp
    }
}
