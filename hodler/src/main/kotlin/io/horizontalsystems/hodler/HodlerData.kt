package io.horizontalsystems.hodler

class HodlerData(val lockTimeInterval: LockTimeInterval, val addressString: String) {

    override fun toString(): String {
        return listOf(lockTimeInterval.serialize(), addressString).joinToString("|")
    }

    companion object {
        fun parse(serialized: String?): HodlerData {
            val (lockTimeIntervalStr, addressString) = checkNotNull(serialized?.split("|"))

            val lockTimeInterval = checkNotNull(LockTimeInterval.deserialize(lockTimeIntervalStr))

            return HodlerData(lockTimeInterval, addressString)
        }
    }
}
