package io.horizontalsystems.bitcoincore.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class FeeRate(val lowPriority: Int, val mediumPriority: Int, val highPriority: Int, val date: Long) {

    @PrimaryKey
    var primaryKey: String = "primary-key"

    companion object {
        //rates for March 19 2019
        val defaultFeeRate = FeeRate(20, 42, 81, 1552970862660)
    }
}
