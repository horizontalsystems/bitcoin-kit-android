package io.horizontalsystems.bitcoinkit.models

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class FeeRate : RealmObject() {

    var lowPriority: Double = 0.0
    var mediumPriority: Double = 0.0
    var highPriority: Double = 0.0
    var dateStr: String = ""
    var date: Long = 0

    @PrimaryKey
    var unique = ""

}
