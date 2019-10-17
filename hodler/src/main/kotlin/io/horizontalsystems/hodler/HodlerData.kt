package io.horizontalsystems.hodler

import io.horizontalsystems.bitcoincore.core.InvalidPluginDataException

class HodlerData(val lockTimeInterval: HodlerPlugin.LockTimeInterval, val addressString: String) {

    override fun toString(): String {
        return listOf(lockTimeInterval.value, addressString).joinToString("|")
    }

    companion object {
        fun parse(serialized: String?): HodlerData {
            val (lockTimeIntervalStr, addressString) = serialized?.split("|") ?: throw InvalidPluginDataException()

            val lockTimeInterval = HodlerPlugin.LockTimeInterval.fromValue(lockTimeIntervalStr.toInt()) ?: throw InvalidPluginDataException()

            return HodlerData(lockTimeInterval, addressString)
        }
    }
}
