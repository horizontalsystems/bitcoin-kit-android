package io.horizontalsystems.hodler

import io.horizontalsystems.bitcoincore.core.IPluginOutputData

class HodlerOutputData(val lockTimeInterval: LockTimeInterval,
                       val addressString: String,
                       val lockedValue: Long) : IPluginOutputData {

    var approxUnlockTime: Long? = null

    fun serialize(): String {
        return listOf(lockTimeInterval.serialize(), addressString, lockedValue).joinToString("|")
    }

    companion object {
        fun parse(serialized: String?): HodlerOutputData {
            val (lockTimeIntervalStr, addressString, valueStr) = checkNotNull(serialized?.split("|"))

            val lockTimeInterval = checkNotNull(LockTimeInterval.deserialize(lockTimeIntervalStr))
            return HodlerOutputData(lockTimeInterval, addressString, valueStr.toLong())
        }
    }
}
