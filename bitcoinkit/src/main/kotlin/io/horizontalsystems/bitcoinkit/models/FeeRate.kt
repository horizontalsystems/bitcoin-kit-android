package io.horizontalsystems.bitcoinkit.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import kotlin.math.max

@Entity
data class FeeRate(val lowPriority: String, val mediumPriority: String, val highPriority: String, val date: Long) {

    @PrimaryKey
    var primaryKey: String = "primary-key"

    val lowest get() = satoshiPerByte(lowPriority)
    val medium get() = satoshiPerByte(mediumPriority)
    val highest get() = satoshiPerByte(highPriority)

    private fun satoshiPerByte(bitcoinPerKB: String): Int {
        return max((bitcoinPerKB.toBigDecimal() * 100_000_000.toBigDecimal() / 1000.toBigDecimal()).toInt(), 1)
    }

    companion object {
        val defaultFeeRate = FeeRate(
                lowPriority = "0.00022165",
                mediumPriority = "0.00043505",
                highPriority = "0.00083333",
                date = 1543211299660
        )
    }
}
