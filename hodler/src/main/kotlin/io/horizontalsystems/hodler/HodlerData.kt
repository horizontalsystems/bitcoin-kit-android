package io.horizontalsystems.hodler

class HodlerData(val lockTimeInterval: HodlerPlugin.LockTimeInterval, val addressString: String) {

    override fun toString(): String {
        return listOf(lockTimeInterval.value, addressString).joinToString("|")
    }

    companion object {
        fun parse(serialized: String?): HodlerData {
            val (lockTimeIntervalStr, addressString) = checkNotNull(serialized?.split("|"))

            val lockTimeInterval = checkNotNull(HodlerPlugin.LockTimeInterval.fromValue(lockTimeIntervalStr.toInt()))

            return HodlerData(lockTimeInterval, addressString)
        }
    }
}
