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

    companion object {
        val defaultFeeRate = FeeRate().apply {
            lowPriority = 20.0
            mediumPriority = 42.0
            highPriority = 81.0
            date = 1552970862660 //rates for March 19 2019
        }
    }
}
