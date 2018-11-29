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

    val lowest get() = satoshiPerByte(lowPriority)
    val medium get() = satoshiPerByte(mediumPriority)
    val highest get() = satoshiPerByte(highPriority)

    private fun satoshiPerByte(bitcoinPerKB: Double): Int {
        return (bitcoinPerKB * 100_000_000 / 1000).toInt()
    }

    companion object {
        val defaultFeeRate = FeeRate().apply {
            lowPriority = 0.00022165
            mediumPriority = 0.00043505
            highPriority = 0.00083333
        }
    }
}
